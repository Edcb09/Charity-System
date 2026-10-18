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

onMounted(()=>fetchPending())



</script>