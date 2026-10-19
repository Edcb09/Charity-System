<template>
    <div style="max-width: 1200px;margin: 20px auto;">
        <el-card>
            <template #header>
                <h3>待审核项目列表</h3>
            </template>

            <el-table :data="pendingList" stripe v-loading="loading">
                <el-table-column prop="title" label="项目名称" width="200" />
                <el-table-column prop="applicantName" label="申请人" width="120" />
                <el-table-column prop="projectType" label="类型" width="100" />
                <el-table-column prop="targetAmount" label="目标金额" width="120" >
                <template #default="scope">￥{{ scope.row.targetAmount }}</template>
                </el-table-column>
                <el-table-column prop="creareTime" label="申请时间" width="180">
                    <template #default="scope">{{ scope.row.createTime.replace('T',' ') }}</template>
                </el-table-column>

                <el-table-column label="操作" width="250" fixed="right">
                    <template #default="scope">
                        <el-button type="success" size="small" @click="handleAudit(scope.row.projectId,2)">通过</el-button>
                        <el-button type="danger" size="small" @click="openRejectDialog(scope.row.projectId)">驳回</el-button>
                    </template>
                </el-table-column>
            </el-table>
        </el-card>

        <!--驳回理由弹窗-->
        <el-dialog v-model="rejectDialogVisible" title="驳回项目" width="400px">
            <el-input v-model="rejectReason" type="textarea" :row="4" placeholder="请输入驳回原因..." />
            <template #footer>
                <el-button @click="rejectDialogVisible=false">取消</el-button>
                <el-button type="danger" @click="confirmReject">确认驳回</el-button>
            </template>
        </el-dialog>
    </div>

    <el-card style="margin-top: 20px;">
        <template #header>
            <h3>资金报告审核</h3>
        </template>
        <el-table :data="fundPendingList" stripe>
            <el-table-column prop="projectId" label="项目ID" width="100" />
            <el-table-column prop="reportAmount" label="支出金额" width="120">
                <template #default="scope">￥{{ scope.row.reportAmount }}</template>
            </el-table-column>
            <el-table-column prop="content" label="使用说明"/>
            <el-table-column label="操作" width="200">
                <template #default="scope">
                    <el-button type="success" size="small" @click="auditFund(scope.row.reportId,1)">通过并公示</el-button>
                    <el-button type="danger" size="small" @click="auditFund(scope.row.reportId,2)">w驳回</el-button>
                </template>
            </el-table-column>
        </el-table>
    </el-card>

</template>

<script setup>
import {ref,onMounted} from 'vue'
import request from '@/utils/request'
import { ElMessage } from 'element-plus';

const pendingList=ref([])
const loading=ref(false)
const rejectDialogVisible=ref(false)
const rejectReason=ref('')
const currentProjectId=ref(null)

const fundPendingList=ref([])

const fetchPending=async()=>
{
    loading.value=true
    try{
        const res=await request.get('/project/admin/pending')
        pendingList.value=res.data
    }catch(e)
    {
        console.error(e)
    }finally{loading.value=false}
}

//审核通过
const handleAudit=async(id,status)=>
{
    try{
        await request.post('/project/admin/audit',{projectId: id, status})
        ElMessage.success("募捐申请已通过审核")
        fetchPending()//刷新列表
    }catch(e){
        console.error(e)
        ElMessage('审核失败')
    }
}

//打开驳回弹窗
const openRejectDialog=(id)=>{
    currentProjectId.value=id
    rejectReason.value=''
    rejectDialogVisible.value=true
}

//确认驳回
const confirmReject=async()=>
{
    if(!rejectReason.value.trim()) return ElMessage.warning("请填写驳回理由")
    try{

        await request.post('/project/admin/audit',{
            projectId:currentProjectId.value,
            status: 4,
            rejectReason: rejectReason.value
        })
        ElMessage.success('已驳回该申请')
        rejectDialogVisible.value=false
        fetchPending()
    }catch(e){console.error(e)}
}

//获取待审核资金报告列表
const fetchFundPending=async()=>{
    try{
        const res=await request.get('/fund-report/admin/pending')
        fundPendingList.value=res.data
    }catch(e)
    {
        console.error("获取待审核资金报告失败",e)
    }
}

//审核资金报告
const auditFund=async(reportId,status)=>{
    if(status===1){
        try{
            await request.post('/fund-report/admin/audit',{reportId,status:1})
            ElMessage.success('资金报告已通过并对外公示')
            fetchFundPending();
        }catch(e)
        {
            console.error(e)
        }
    }else if(status===2)
    {
        ElMessageBox.prompt('请输入驳回理由','驳回资金报告',{
            confirmButtonText:'确定',
            cancelButtonText:'取消',
            imputPattern:/\S+/,//正则校验，不能为空
            inputErrorMessage: '驳回理由不能为空',
        }).then(async({value})=>{
            try{
                await request.post('/fund-report/admin/aduit',{
                    reportId,
                    status: 2,
                    rejectReason: value
                })
                ElMessage.success('已驳回该资金报告')
                fetchFundPending()
            }catch(e)
            {
                console.error(e)
            }
        }).catch(()=>{
            //用户点击取消，不做处理
        })
    }
}


onMounted(()=>
{
    fetchPending()
    fetchFundPending()
})


</script>