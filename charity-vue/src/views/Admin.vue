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

        <!-- 举报处理面板 -->
    <el-card style="margin-top: 20px;">
      <template #header><h3>用户举报处理</h3></template>
      <el-table :data="reportList" stripe v-loading="loading">
        <el-table-column prop="projectTitle" label="被举报项目" min-width="150" />
        <el-table-column prop="reporterName" label="举报人" width="120" />
        <el-table-column prop="reason" label="举报理由" width="150" />
        <el-table-column prop="createTime" label="举报时间" width="160">
          <template #default="scope">{{ scope.row.createTime.replace('T', ' ') }}</template>
        </el-table-column>
        <el-table-column label="状态" width="100">
          <template #default="scope">
            <el-tag v-if="scope.row.status === 0" type="warning">待处理</el-tag>
            <el-tag v-if="scope.row.status === 1" type="success">已处理</el-tag>
            <el-tag v-if="scope.row.status === 2" type="info">已驳回</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="160" fixed="right">
          <template #default="scope">
            <el-button 
              v-if="scope.row.status === 0" 
              type="primary" 
              size="small" 
              @click="openHandleDialog(scope.row)"
            >去处理</el-button>
            <span v-else style="color: #999; font-size: 13px;">结果：{{ scope.row.handleResult }}</span>
          </template>
        </el-table-column>
      </el-table>
    </el-card>


    <!-- 处理举报弹窗 -->
    <el-dialog v-model="handleDialogVisible" title="处理举报" width="400px">
      <el-form label-width="80px">
        <el-form-item label="处理结果">
          <el-radio-group v-model="handleForm.status">
            <el-radio :label="1">举报属实</el-radio>
            <el-radio :label="2">恶意举报/驳回</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="结果说明">
          <el-input v-model="handleForm.handleResult" type="textarea" :rows="3" placeholder="比如：已核实违规，项目已下架..." />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="handleDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="submitHandle">提交处理</el-button>
      </template>
    </el-dialog>

</template>

<script setup>
import {ref,onMounted,reactive} from 'vue'
import request from '@/utils/request'
import { ElMessage, ElMessageBox } from 'element-plus'



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
        setTimeout(() => window.location.reload(), 1000) 
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
        setTimeout(() => window.location.reload(), 1000) 
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
            setTimeout(() => window.location.reload(), 1000) 
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
                setTimeout(() => window.location.reload(), 1000) 
            }catch(e)
            {
                console.error(e)
            }
        }).catch(()=>{
            //用户点击取消，不做处理
        })
    }
}


// --- 举报处理相关 ---
const reportList = ref([])
const handleDialogVisible = ref(false)
const handleForm = reactive({
  reportId: null,
  status: 1, // 默认属实
  handleResult: ''
})

// 获取举报列表
const fetchReports = async () => {
  try {
    const res = await request.get('/report/admin/list')
    reportList.value = res.data
  } catch (error) { console.error(error) }
}

// 打开处理弹窗
const openHandleDialog = (row) => {
  handleForm.reportId = row.reportId
  handleForm.status = 1
  handleForm.handleResult = ''
  handleDialogVisible.value = true
}

// 提交处理
const submitHandle = async () => {
  if (!handleForm.handleResult.trim()) return ElMessage.warning('请填写结果说明')
  try {
    await request.post('/report/admin/handle', handleForm)
    ElMessage.success('举报处理完成')
    handleDialogVisible.value = false
    fetchReports() // 刷新列表
    // 刷新页面让顶部红点数字更新
    setTimeout(() => window.location.reload(), 1000) 
  } catch (error) { console.error(error) }
}


onMounted(() => {
  fetchPending()
  fetchFundPending()
  fetchReports() 
})



</script>