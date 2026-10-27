<template>
    <div class="detail-container" v-if="project.projectId">
        <el-card>
            <el-page-header @back="$router.push('/')" content="募捐详情" style="margin-bottom: 20px;" />
            <el-row :gutter="30">
                <!--左侧图片与信息-->
                <el-col :xs="24" :sm="10" style="overflow: hidden;">
                <img :src="project.coverImg || 'https://via.placeholder.com/500x300' "
                @error="(e)=>e.target.src='https://via.placeholder.com/500x300?text=No+Image'"
                class="detail-img" />
                </el-col>

                <!--右侧核心信息-->
                <el-col :xs="24" :sm="14">
                    <h1>{{ project.title }}</h1>
          <p class="type-tag">项目类型：{{ project.projectType }}</p>
          
                <!--时间信息展示 -->
                <div class="time-info" style="margin-bottom: 15px; font-size: 13px; color: #666;">
                <!-- 开始时间（审核通过的时间，如果没有就用创建时间） -->
                <span style="margin-right: 20px;" v-if="project.status >= 2 && project.status !== 4">
                  ⏱ 开始募捐：{{ (project.auditTime || project.createTime).replace('T', ' ').substring(0, 16) }}
                </span>
            
                <!-- 结束时间逻辑：如果是募捐中，显示预期；如果已结束/归档，显示实际达标/结束时间 -->
                <span v-if="project.status === 2">
                  ⏳ 预期截止：{{ project.endTime ? project.endTime.replace('T', ' ').substring(0, 16) : '-' }}
                </span>
                <span v-if="project.status === 3 || project.status === 5">
                  🏁 结束时间：{{ (project.finishTime || project.endTime).replace('T', ' ').substring(0, 16) }}
                </span>
              </div>
                         
                    <!-- 举报按钮 -->
                    <el-button type="danger" plain size="small" @click="openViolationDialog()">举报</el-button>

                    <el-progress
                    :percentage="calculatePercentage"
                    :stroke-width="15"
                    style="margin:20px 0"
                    />

                    <div class="amount-box">
                        <div>
                            <span class="label">已筹集金额</span>
                            <span class="value highlight">￥{{ project.currentAmount }}</span>
                        </div>
                        <div>
                            <span class="label">目标金额</span>
                            <span class="value">￥{{ project.targetAmount }}</span>
                        </div>
                    </div>

                    
                    <template v-if="project.status===2">
                    <el-divider />

                    <div class="action-box" v-if="role !== '2'">
                        <el-input-number v-model="donateAmount" :min="1" :max="999999" label="捐款金额" />
                        <el-checkbox v-model="isAnonymous" style="margin-left: 20px;">匿名捐款</el-checkbox>
                        <el-button type="danger" size="large" style="width: 100%;margin-top: 20px;"@click="handleDonate">
                            立即捐款
                        </el-button>
                    </div>
                </template>

                    <!-- 管理员强制下架：募捐中(2) / 已结束(3) / 结项待审核(6) 均可下架。
                         已归档(5) 是合规终态，下架会抹掉历史资金公示，故不提供 -->
                    <el-button
                    v-if="role === '2' && [2,3,6].includes(project.status)"
                    type="danger"
                    size="large"
                    plain
                    style="width: 100%; margin-top: 20px;"
                    @click="handleAdminOffline"
                    >强制下架该项目</el-button>
                </el-col>
            </el-row>

            <el-divider content-position="left">募捐详情介绍</el-divider>
            <p class="description">{{ project.description }}</p>

            <el-divider content-position="left">受助人信息</el-divider>
            <p class="description">{{ project.recipientInfo }}</p>
            
        </el-card>

                  <!-- 在捐款操作区下方加上 -->
          <div style="margin-top: 20px; background: #fff; padding: 15px; border-radius: 8px; border: 1px solid #ebeef5;">
            <h4 style="margin: 0 0 10px 0; color: #333; font-size: 14px;">💖 最新爱心榜</h4>
            <div v-if="donorList.length === 0" style="font-size: 13px; color: #999;">暂无捐赠记录，快来做第一个吧~</div>
            <div v-for="(item, index) in donorList" :key="index" style="font-size: 13px; color: #666; margin-bottom: 5px; display: flex; justify-content: space-between;">
              <span>{{ item.donorName }}</span>
              <span style="color: #F56C6C; font-weight: bold;">￥{{ item.amount }}</span>
            </div>
          </div>

        <!--模拟支付弹窗-->
        <el-dialog v-model="showPayDialog" title="支付确认" width="400px" :close-on-click-modal="false">
            <div style="text-align: center;">
                <h2 style="color: #f56c6c;">￥{{ donateAmount }}</h2>
                <p>请确认捐款金额</p>
            </div>
            <template #footer>
                <el-button @click="showPayDialog = false">取消</el-button>
                <el-button type="primary" @click="confirmPay" :loading="payLoading">支付</el-button>
            </template>
        </el-dialog>
    </div>


    <!--资金公示区域-->
    <el-card style="margin-top: 20px;" v-if="(project.status === 3||project.status===5||project.status===6)&&project.currentAmount>0">
            <!-- 财务汇总面板 -->
      <div class="fund-summary" v-if="fundSummary">
        <div class="summary-item">
          <span class="label">总筹款：</span>
          <span class="value text-primary">￥{{ fundSummary.totalRaised }}</span>
        </div>
        <div class="summary-item">
          <span class="label">已公示支出：</span>
          <span class="value text-danger">￥{{ fundSummary.totalExpended }}</span>
        </div>
        <div class="summary-item">
          <span class="label">当前结余：</span>
          <span class="value text-warning">￥{{ fundSummary.balance }}</span>
        </div>
      </div>
        <el-divider content-position="left">资金使用公示</el-divider>
        <el-alert 
        v-if="project.status === 3 && project.currentAmount < project.targetAmount"
        title="该项目未达到目标金额，已提前结束募捐。筹得的全部善款将直接用于受助人，受助人需按照实际筹集金额进行资金公示。"
        type="warning"
        show-icon
        :closable="false"
        style="margin-bottom: 20px;"
      />
        <el-empty v-if="fundReportList.length===0" description="暂无资金公示记录" />
        <div v-for="item in fundReportList" :key="item.reportId" class="fund-item">
            <h4>支出金额：<span style="color:#f56c6c">￥{{ item.reportAmount }}</span></h4>
            <p>使用说明：{{ item.content }}</p>
            <p class="time">公示时间：{{ item.createTime.replace('T',' ') }}</p>
    <!-- 解析并展示文件列表 -->
        <div v-if="item.reportImg" class="fund-files">
          <span style="font-size: 13px; color: #666; margin-right: 10px;">相关凭证：</span>
          <el-button 
            v-for="(file, idx) in parseFiles(item.reportImg)" 
            :key="idx" 
            type="primary" 
            link 
            size="small"
            @click="previewFile(file.url)"
          >
            📄 {{ file.name }}
          </el-button>
        </div>
        </div>

        <!--申请人提交表单和结项入口（仅项目申请人可见）-->
        <el-button
        v-if="currentUsername===project.applicantName&&project.status===3"
        type="primary"
        style="margin-top: 15px;"
        @click="showFundReportDialog=true"
        >提交资金使用报告及证明材料</el-button>

        <el-button 
        v-if="currentUserId === project.userId && project.status === 3"
        type="success" 
        style="margin-top: 15px;"
        @click="handleApplyFinish"
      >申请结项归档</el-button>
      
      <el-alert
      v-if="project.status===6"
      title="该项目正在等待管理员进行归档审核，暂时无法提交新报告"
      type="info"
      show-icon
      style="margin-top:15px"
      />
    </el-card>

    <el-card style="margin-top: 20px;" v-else-if="(project.status === 3 || project.status === 5) && project.currentAmount == 0">
      <el-empty description="很遗憾，该项目未能筹集到任何善款，已自动归档结束。感谢您的关注。" />
    </el-card>

        <!-- 资金报告提交弹窗 -->
    <el-dialog v-model="showFundReportDialog" title="提交资金使用报告" width="500px">
      <el-form label-width="100px">
        <el-form-item label="支出金额">
          <el-input-number v-model="fundReportForm.reportAmount" :min="1" />
        </el-form-item>
        <el-form-item label="使用说明">
          <el-input v-model="fundReportForm.content" type="textarea" :rows="3" />
        </el-form-item>
        <el-form-item label="上传凭证">
          <div style="width: 100%;">
            <el-upload
              class="upload-file"
              :show-file-list="false"
              :http-request="uploadFundFile"
              accept=".jpg,.jpeg,.png,.pdf,.doc,.docx,.xls,.xlsx,.zip"
            >
              <el-button type="primary" plain>点击上传凭证 (可多次点击上传)</el-button>
              <template #tip>
                <div class="el-upload__tip">支持图片、PDF、Word、Excel，单文件不超过 10MB</div>
              </template>
            </el-upload>
            
            <!-- 已上传的文件列表 -->
            <div v-if="fundReportFileList.length > 0" class="uploaded-file-list" style="margin-top: 10px;">
              <div v-for="(file, index) in fundReportFileList" :key="index" class="file-item">
                <span class="file-name">📄 {{ file.name }}</span>
                <div>
                  <el-button type="primary" link size="small" @click="previewFile(file.url)">预览</el-button>
                  <el-button type="danger" link size="small" @click="removeFile(index)">删除</el-button>
                </div>
              </div>
            </div>
          </div>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="showFundReportDialog = false">取消</el-button>
        <el-button type="primary" @click="submitFundReport">提交</el-button>
      </template>
    </el-dialog>

    <!--留言板区域-->
    <el-card style="margin-top: 20px;" v-if="project.status === 2 || project.status === 3 || project.status === 5 ||project.status===6">
        <el-divider content-position="left">留言板({{ commentList.length }})</el-divider>

        <!--发布留言-->
        <div class="comment-input-box">
            <el-input
            v-model="newComment"
            type="textarea"
            :rows="3"
            placeholder="留下您的鼓励与支持..."
            maxlength="500"
            show-word-limit />
            <el-button type="primary" style="margin-top: 10px;float:right;"@click="submitComment">发布留言</el-button>
            <div style="clear: both;"></div>
        </div>

        <!-- 留言列表 -->
      <div class="comment-list" style="margin-top: 30px;">
        <el-empty v-if="commentList.length === 0" description="暂无留言，快来抢沙发吧~" />
        
        <div v-for="item in commentList" :key="item.commentId" class="comment-item">
          <!-- 左侧：头像 -->
          <el-avatar 
            :size="40" 
            :src="item.avatar || 'https://cube.elemecdn.com/3/7c/3ea6beec64369c2642b92c6726f1epng.png'" 
            class="comment-avatar" 
          />
          
          <!-- 右侧：内容与信息 -->
          <div class="comment-right">
            <div class="comment-header">
              <span class="username">{{ item.username }}</span>
              <div class="comment-actions">
                <span class="time">{{ formatCommentTime(item.createTime) }}</span>

                <!-- 删除自己留言的按钮 -->
                <el-button 
                  v-if="item.userId === currentUserId" 
                  type="primary" 
                  link 
                  size="small" 
                  style="margin-left: 10px;"
                  @click="handleDeleteComment(item.commentId)"
                >删除</el-button>

                <el-button 
                  type="danger" 
                  link 
                  size="small" 
                  style="margin-left: 10px;"
                  @click="openCommentReport(item)"
                >举报</el-button>
              </div>
            </div>
            <div class="comment-content">{{ item.content }}</div>
          </div>
        </div>
      </div>
    </el-card>

    <!-- 举报弹窗 -->
    <el-dialog v-model="showViolationDialog" title="举报违规留言" width="400px">
      <el-form label-width="80px">
        <el-form-item label="举报理由" required>
          <el-select v-model="violationForm.reason" placeholder="请选择或输入理由" style="width: 100%;" filterable allow-create>
            <el-option label="涉嫌诈骗" value="涉嫌诈骗" />
            <el-option label="信息虚假" value="信息虚假" />
            <el-option label="涉及色情暴力" value="涉及色情暴力" />
            <el-option label="其他" value="其他" />
          </el-select>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="showViolationDialog = false">取消</el-button>
        <el-button type="danger" @click="submitReport">确认举报</el-button>
      </template>
    </el-dialog>

</template>

<script setup>
import {ref,onMounted,computed,reactive} from 'vue'
import {useRoute,useRouter} from 'vue-router'
import request from '@/utils/request'
import { ElMessage, valueEquals,ElMessageBox } from 'element-plus';
import { useUserStore } from '@/stores/user';
import { useMessageStore } from '@/stores/message';

const route=useRoute()
const router=useRouter()
const userStore=useUserStore()
const messageStore=useMessageStore()

const project=ref({})
const donateAmount =ref(10)
const isAnonymous =ref(false)
const showPayDialog=ref(false)
const payLoading=ref(false)
const currentOrderNo=ref('')

const fundReportList=ref([])//公示列表
const showFundReportDialog=ref(false)//提交弹窗

const currentUserId = ref(Number(localStorage.getItem('userId'))) // 存入当前用户ID
const role = computed(() => localStorage.getItem('role') || '0')

const fundReportForm = reactive({
  projectId: null,
  reportAmount: 100, 
  content: '',
  reportImg: '' // 提交时用于存 JSON 字符串
})

const fundReportFileList = ref([]) 

const currentUsername=ref(localStorage.getItem('username') || '')

const newComment=ref('')
const commentList=ref([])

//计算进度
const calculatePercentage=computed(()=>{
    if(!project.value.targetAmount) return 0
    let p=(project.value.currentAmount/project.value.targetAmount)*100
    return p>100?100:Math.floor(p)
})

//获取详情
const fetchDetail =async()=>{
    try{
        const res=await request.get(`/project/${route.params.id}`)
        project.value=res.data
        if ([3, 5, 6].includes(project.value.status)) {
            fetchFundSummary()
        }
    }catch(error){
        console.error(error)
    }
}

const handleAdminOffline = () => {
  // 已结束/待归档的项目下架时，提醒管理员：历史流水保留，已筹资金不做自动处理
  const tip = project.value.status === 2
    ? '确定要强制下架该项目吗？此操作不可逆。'
    : '确定要强制下架该项目吗？该项目已结束，下架后仍保留捐款流水与资金公示记录，已筹资金不做自动处理。'
  ElMessageBox.confirm(tip, '强制下架', {
    confirmButtonText: '确定下架',
    cancelButtonText: '取消',
    type: 'error',
  }).then(async () => {
    try {
      await request.post(`/project/admin/offline/${project.value.projectId}`)
      ElMessage.success('项目已强制下架')
      fetchDetail() // 刷新页面
    } catch (error) { console.error(error) }
  }).catch(() => {})
}

//点击捐款
const handleDonate=async()=>
{
    const token=localStorage.getItem('satoken')
    if(!token){
        ElMessage.warning('请先登录')
        userStore.openLoginDialog()
        return
    }

    if(donateAmount.value<=0)
    {
        return ElMessage.warning('请输入正确的捐款金额')
    }
    
    try{
        //创建捐款
        const res=await request.post('/donation/create',{
            projectId: project.value.projectId,
            amount: donateAmount.value,
            isAnonymous: isAnonymous.value
        })
        currentOrderNo.value=res.data
        showPayDialog.value=true//弹出支付确认
    }catch(error)
    {
        console.error(error)
    }
}
        
//确认支付
const confirmPay = async()=>
{
    payLoading.value=true
    try{
        //2.模拟支付成功
        await request.post('/donation/pay',{orderNo:currentOrderNo.value})
        ElMessage.success('捐款成功！感谢您的爱心！')
        showPayDialog.value=false
        fetchDetail()//刷新页面数据
        messageStore.fetchUnreadCount()
    }catch(error)
    {
        console.error(error)
    }finally{
        payLoading.value=false
    }
}

const donorList = ref([])

const fetchDonorList = async () => {
  try {
    const res = await request.get(`/donation/list/${route.params.id}`)
    donorList.value = res.data
  } catch (error) { console.error(error) }
}



//获取留言
const fetchComments=async()=>
{
    try{
        const res=await request.get(`/comment/list/${route.params.id}`)
        commentList.value=res.data
    }catch(error)
    {
        console.error(error)
    }
}

//提交留言
const submitComment =async()=>
{
    const token=localStorage.getItem('satoken')
    if(!token){
        ElMessage.warning('请先登录后再留言~')
        userStore.openLoginDialog()
        return
    }

    if(!newComment.value.trim())
    {
        return ElMessage.warning('留言内容不能为空')
    }

    try{
        await request.post('/comment/add',{
            projectId:project.value.projectId,
            content:newComment.value
        })
        ElMessage.success('留言成功!')
        newComment.value=''
        fetchComments()
    }catch(error){
        console.error(error)
    }
}

// 删除留言
const handleDeleteComment = (commentId) => {
  ElMessageBox.confirm('确定要删除这条留言吗？', '提示', {
    confirmButtonText: '确定',
    cancelButtonText: '取消',
    type: 'warning',
  }).then(async () => {
    try {
      await request.delete(`/comment/${commentId}`)
      ElMessage.success('留言已删除')
      fetchComments() // 重新加载留言列表
    } catch (error) {
      console.error(error)
    }
  }).catch(() => {
    // 用户取消删除
  })
}

//格式化留言时间
const formatCommentTime=(timeStr)=>{
    if(!timeStr) return ''

    //转换时间格式
    const time=new Date(timeStr)
    if(isNaN(time.getTime())){
        return ''
    }
    const now=new Date()
    const diffMs=now-time //时间差（毫秒）
    const diffSec=Math.floor(diffMs/1000)
    const diffMin=Math.floor(diffSec/60)
    const diffHour=Math.floor(diffMin/60)
    const diffDay=Math.floor(diffHour/24)

    if(diffSec<60){
        return '刚刚'
    }else if(diffMin<60){
        return `${diffMin}分钟前`
    }else if(diffHour<24){
        return `${diffHour}小时前`
    }else if(diffDay<=3)
    {
        return `${diffDay}天前`
    }else{
        //超过3天，只显示日期
        const year=time.getFullYear()
        const month=String(time.getMonth()+1).padStart(2,'0')
        const day=String(time.getDate()).padStart(2,'0')
        return `${year}-${month}-${day}`
    }
}



const fundSummary = ref(null)

const fetchFundSummary = async () => {
  try {
    const res = await request.get(`/fund-report/summary/${route.params.id}`)
    fundSummary.value = res.data
  } catch (error) { console.error(error) }
}


//获取项目的资金公示列表
const fetchFundReports =async()=>
{
    try{
        const res=await request.get(`/fund-report/list/${route.params.id}`)
        fundReportList.value=res.data
    }catch(e)
    {
        console.error("获取资金公示资料失败",e)
    }
}

//提交资金使用报告
const submitFundReport = async () => {
  if (!fundReportForm.reportAmount || fundReportForm.reportAmount <= 0) {
    return ElMessage.warning('请输入正确的支出金额')
  }
  if (!fundReportForm.content.trim()) {
    return ElMessage.warning('请填写资金使用说明')
  }

  try {
    fundReportForm.projectId = project.value.projectId
    // 把文件列表转为 JSON 字符串存入 reportImg
    fundReportForm.reportImg = fundReportFileList.value.length > 0 
      ? JSON.stringify(fundReportFileList.value) 
      : ''
      
    await request.post('/fund-report/add', fundReportForm)
    ElMessage.success('报告提交成功，等待管理员审核')
    
    showFundReportDialog.value = false
    fundReportForm.reportAmount = 100
    fundReportForm.content = ''
    fundReportFileList.value = [] // 清空文件列表
    fetchFundReports()
  } catch (error) {
    console.error("提交资金报告失败", error)
  }
}

// 上传资金凭证（兼容图片和PDF）
const uploadFundFile = async (options) => {
  const file = options.file
  const isLt10M = file.size / 1024 / 1024 < 10
  if (!isLt10M) return ElMessage.error('文件大小不能超过 10MB!')

  const formData = new FormData()
  formData.append('file', file)
  
  try {
    const res = await request.post('/file/upload', formData, {
      headers: { 'Content-Type': 'multipart/form-data' }
    })
    // 将文件名和 URL 存入列表
    fundReportFileList.value.push({
      name: file.name,
      url: res.data
    })
    ElMessage.success('文件上传成功')
  } catch (error) {
    ElMessage.error('上传失败')
  }
}

// 移除文件
const removeFile = (index) => {
  fundReportFileList.value.splice(index, 1)
}

// 预览文件（图片直接看，文档新窗口打开或下载）
const previewFile = (url) => {
  const isImage = /\.(jpg|jpeg|png|gif)$/i.test(url)
  if (isImage) {
    // 如果是图片，可以用 Element Plus 的图片预览
    // 这里简单处理，直接新窗口打开
    window.open(url, '_blank')
  } else {
    // 如果是 PDF/Word 等，新窗口打开（浏览器会提示下载或直接用自带阅读器打开）
    window.open(url, '_blank')
  }
}

// 解析文件列表
const parseFiles = (jsonStr) => {
  if (!jsonStr) return []
  // 兼容以前单张图片的字符串
  if (jsonStr.startsWith('http') || jsonStr.startsWith('/images')) {
    return [{ name: '查看凭证图片', url: jsonStr }]
  }
  try {
    return JSON.parse(jsonStr)
  } catch (e) {
    return []
  }
}

//资金报告完毕后申请结项
const handleApplyFinish = async () => {
    if (!fundSummary.value) {
    await fetchFundSummary()
  }
  if (fundSummary.value && fundSummary.value.balance > 0) {
    return ElMessageBox.alert(
      `当前项目仍有 ￥${fundSummary.value.balance} 元资金未公示支出，请先补充资金使用报告，待结余为 0 后方可申请结项。`,
      '无法申请结项',
      { confirmButtonText: '我知道了', type: 'warning' }
    )
  }
  ElMessageBox.confirm(
    '请确保所有资金使用报告已提交完毕。归档后，项目页面将变为只读状态，不可再操作。确定申请结项吗？', 
    '申请结项', 
    {
      confirmButtonText: '确定申请',
      cancelButtonText: '取消',
      type: 'warning',
    }
  ).then(async () => {
    try {
      const res = await request.post(`/project/applyFinish/${project.value.projectId}`)
      ElMessage.success(res.msg)
      fetchDetail() 
    } catch (error) {
      console.error(error)
    }
  }).catch(() => {})
}


//举报模块
const showViolationDialog = ref(false) 
const violationForm = reactive({ reason: '' ,targetType:1,commentId:null}) 

// 打开举报弹窗
const openViolationDialog = (comment=null) => {
  if (!localStorage.getItem('satoken')) {
    ElMessage.warning('请先登录后再举报')
    userStore.openLoginDialog()
    return
  }
  if (comment) {
    // 举报留言
    violationForm.targetType = 2
    violationForm.commentId = comment.commentId
  } else {
    // 举报项目
    violationForm.targetType = 1
    violationForm.commentId = null
  }
  showViolationDialog.value = true 
}

const openCommentReport = (comment) => {
  openViolationDialog(comment)
}

// 提交举报方法
const submitReport = async () => {
  if (!violationForm.reason) return ElMessage.warning('请选择或输入举报理由')
  
  try {
    await request.post('/report/add', {
      projectId: project.value.projectId,
      reason: violationForm.reason,
      targetType: violationForm.targetType, 
      commentId: violationForm.commentId    
    })
    ElMessage.success('举报提交成功，管理员会尽快处理')
    
    showViolationDialog.value = false 
    violationForm.reason = '' 
    violationForm.commentId=null
    violationForm.targetType=1
  } catch (error) {
    console.error(error)
  }
}




onMounted(()=>{
    fetchDetail()
    fetchComments()//加载留言
    fetchFundReports()
    fetchDonorList()
  //   if ([3, 5, 6].includes(project.value.status)) {
  //   fetchFundSummary()
  // }
})
</script>

<style scoped>
.detail-container{max-width: 1000px;margin: 20px auto;}
.detail-img {
  width: 100%;
  height: 350px; 
  object-fit: cover; 
  border-radius: 8px;
  display: block;
}
h1{font-size: 24px;margin-bottom: 10px;}
.type-tag{color: #909299;font-size: 14px;background: #f4f4f5;display: inline-block;padding: 2px 8px;border-radius: 4px;}
.amount-box{display: flex;justify-content: space-between;background: #f8f9fa;padding: 15px;border-radius: 8px;margin-bottom: 20px;}
.label{display: block;font-size: 14px;color: #666;margin-bottom: 5px;}
.value{font-size: 20px;font-weight: bold;}
.highlight{color:#f56c6c}
.description{line-height: 1.8;color: #555;text-indent: 2em;}

.comment-input-box{
    background: #f8f9fa;
    padding: 15px;
    border-radius: 8px;
}
/* 留言列表布局 */
.comment-item {
  padding: 15px 0;
  border-bottom: 1px dashed #ebeef5;
  display: flex; /* 让头像和内容左右排列 */
  align-items: flex-start; /* 顶部对齐 */
}
.comment-item:last-child {
  border-bottom: none;
}

.comment-avatar {
  margin-right: 15px;
  flex-shrink: 0; /* 防止头像被挤压变形 */
  background-color: #f0f2f5; /* 默认头像的底色 */
}

.comment-right {
  flex: 1; /* 让右侧内容占满剩余空间 */
}

.comment-header {
  display: flex;
  justify-content: space-between;
  margin-bottom: 5px;
}

.comment-header .username {
  font-weight: bold;
  color: #409EFF;
  font-size: 14px;
}

.comment-actions {
  display: flex;
  align-items: center;
}

.comment-header .time {
  font-size: 12px;
  color: #909399;
}

.comment-content {
  color: #333;
  line-height: 1.6;
  font-size: 14px;
  word-break: break-all; /* 防止长文本撑破容器 */
}

.fund-summary {
  display: flex;
  justify-content: space-around;
  background-color: #f8f9fa;
  padding: 15px;
  border-radius: 8px;
  margin-bottom: 20px;
}
.summary-item {
  text-align: center;
}
.summary-item .label {
  font-size: 13px;
  color: #666;
  display: block;
  margin-bottom: 5px;
}
.summary-item .value {
  font-size: 18px;
  font-weight: bold;
}
.text-primary { color: var(--el-color-primary); }
.text-danger { color: #F56C6C; }
.text-warning { color: #E6A23C; }
</style>