<template>
    <div class="detail-container" v-if="project.projectId">
        <el-card>
            <el-page-header @back="$router.push('/')" content="募捐详情" style="margin-bottom: 20px;" />
            <el-row :gutter="30">
                <!--左侧图片与信息-->
                <el-col :xs="24" :sm="10" style="overflow: hidden;">
                <img :src="project.coverImg || 'https://via.placeholder.com/500x300' "
                @error="(e)=>e.target.src='https://via.placeholder.com/500x300?text=No+Image'"
                "class="detail-img" />
                </el-col>

                <!--右侧核心信息-->
                <el-col :xs="24" :sm="14">
                    <h1>{{ project.title }}</h1>
                    <p class="type-tag">项目类型：{{ project.projectType }}</p>
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
                    <div class="action-box">
                        <el-input-number v-model="donateAmount" :min="1" :max="999999" label="捐款金额" />
                        <el-checkbox v-model="isAnonymous" style="margin-left: 20px;">匿名捐款</el-checkbox>
                        <el-button type="danger" size="large" style="width: 100%;margin-top: 20px;"@click="handleDonate">
                            立即捐款
                        </el-button>
                    </div>
                </template>
                </el-col>
            </el-row>

            <el-divider content-position="left">募捐详情介绍</el-divider>
            <p class="description">{{ project.description }}</p>

            <el-divider content-position="left">受助人信息</el-divider>
            <p class="description">{{ project.recipientInfo }}</p>
            
        </el-card>

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
    <el-card style="margin-top: 20px;" v-if="project.status === 3||project.status===5||project.status===6">
        <el-divider content-position="left">资金使用公示</el-divider>
        <el-empty v-if="fundReportList.length===0" description="暂无资金公示记录" />
        <div v-for="item in fundReportList" :key="item.reportId" class="fund-item">
            <h4>支出金额：<span style="color:#f56c6c">￥{{ item.reportAmount }}</span></h4>
            <p>使用说明：{{ item.content }}</p>
            <p class="time">公示时间：{{ item.createTime.replace('T',' ') }}</p>
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
          <el-upload
            class="upload-file"
            :show-file-list="false"
            :http-request="uploadFundFile"
            accept=".jpg,.jpeg,.png,.pdf"
          >
            <el-button type="primary" plain>点击上传图片或PDF</el-button>
            <template #tip>
              <div class="el-upload__tip">支持 JPG/PNG/PDF，不超过 5MB</div>
            </template>
          </el-upload>
          <!-- 如果有文件了，显示文件名或预览图 -->
          <div v-if="fundReportForm.reportImg" style="margin-top: 10px;">
            <span style="color: #67C23A;">✓ 已上传文件</span>
            <a :href="fundReportForm.reportImg" target="_blank" style="margin-left: 10px; color: #409EFF;">点击预览</a>
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

                <!-- 删除自己留言的按钮 👇 -->
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
    <el-dialog v-model="showViolationDialog" title="举报违规项目" width="400px">
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

const fundreportForm=reactive({
    projectId:null,
    reportAmount:100,
    content:''
})
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
    }catch(error){
        console.error(error)
    }
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
const submitFundReport =async()=>
{
    //基础校验
    if(!fundreportForm.reportAmount||fundreportForm.reportAmount<=0){
        return ElMessage.warning('请输入正确的支出金额')
    }
    if(!fundreportForm.content.trim()){
        return ElMessage.warning('请填写资金使用说明')
    }

    try{
        fundreportForm.projectId=project.value.projectId

        await request.post('/fund-report/add',fundreportForm)
        ElMessage.success('报告提交成功，等待管理员审核')

        //关闭弹窗，清空表单，并刷新列表
        showFundReportDialog.value=false
        fundreportForm.reportAmount=100
        fundreportForm.content=''
        fetchFundReports()
    }catch(e)
    {
        console.error("提交资金报告失败",e)
    }
}

// 上传资金凭证（兼容图片和PDF）
const uploadFundFile = async (options) => {
  const formData = new FormData()
  formData.append('file', options.file)
  try {
    // 后端不需要改，它只管存文件
    const res = await request.post('/file/upload', formData, {
      headers: { 'Content-Type': 'multipart/form-data' }
    })
    fundReportForm.reportImg = res.data // 存下 URL
    ElMessage.success('凭证上传成功')
  } catch (error) {
    ElMessage.error('上传失败')
  }
}

//资金报告完毕后申请结项
const handleApplyFinish = () => {
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
})
</script>

<style scoped>
.detail-container{max-width: 1000px;margin: 20px auto;}
.detail-img{
    width: 100%;
    max-width: 100%;
    height: auto;
    border-radius: 8px;
    object-fit: cover;}
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
</style>