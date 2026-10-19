<template>
    <div class="=detail-container" v-if="project.projectId">
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
    <el-card style="margin-top: 20px;" v-if="project.status === 3||project.status===5">
        <el-divider content-position="left">资金使用公示</el-divider>
        <el-empty v-if="fundReportList.length===0" description="暂无资金公示记录" />
        <div v-for="item in fundReportList" :key="item.reportId" class="fund-item">
            <h4>支出金额：<span style="color:#f56c6c">￥{{ item.reportAmount }}</span></h4>
            <p>使用说明：{{ item.content }}</p>
            <p class="time">公示时间：{{ item.createTime.replace('T',' ') }}</p>
        </div>

        <!--申请人提交表单入口（仅项目申请人可见）-->
        <el-button
        v-if="currentUsername===project.applicantName&&project.status===3"
        type="primary"
        style="margin-top: 15px;"
        @click="showReportDialog=true"
        >提交资金使用报告及证明材料</el-button>
    </el-card>

    <!--提交资金弹窗-->
    <el-dialog v-model="showReportDialog" title="提交资金使用报告" width="500px">
        <el-form label-width="100px">
            <el-form-item label="支出金额">
                <el-input-number v-model="reportForm.reportAmount" :min="1"/>
            </el-form-item>
            <el-form-item label="支出说明">
                <el-input v-model="reportForm.content" type="textarea" :rows="3" />
            </el-form-item>
        </el-form>
        <template #footer>
            <el-button @click="showReportDialog=false">取消</el-button>
            <el-button type="primary" @click="submitFundReport">提交</el-button>
        </template>
    </el-dialog>

    <!--留言板区域-->
    <el-card style="margin-top: 20px;">
        <el-divider content-position="left">留言板({{ commentList.length }})</el-divider>

        <!--发布留言-->
        <div class="comment-input-box">
            <el-input
            v-model="newComment"
            type="textarea"
            :row="3"
            placeholder="留下您的鼓励与支持..."
            maxlength="500"
            show-word-limit />
            <el-button type="primary" style="margin-top: 10px;float:right;"@click="submitComment">发布留言</el-button>
            <div style="clear: both;"></div>
        </div>

        <!--留言板-->
        <div class="comment-list" style="margin-top:30px">
            <el-empty v-if="commentList.length===0" description="暂无留言，快来抢沙发吧~"/>
            <div v-for="item in commentList" :key="item.commentId" class="comment-item">
                <div class="comment-header">
                    <span class="username">{{ item.username }}</span>
                    <span class="time">{{ formatCommentTime(item.createTime) }}</span>
                </div>
                <div class="comment-content">{{ item.content }}</div>
            </div>
        </div>
    </el-card>

</template>

<script setup>
import {ref,onMounted,computed,reactive} from 'vue'
import {useRoute,useRouter} from 'vue-router'
import request from '@/utils/request'
import { ElMessage, valueEquals } from 'element-plus';
import { useUserStore } from '@/stores/user';

const route=useRoute()
const router=useRouter()
const userStore=useUserStore()

const project=ref({})
const donateAmount =ref(10)
const isAnonymous =ref(false)
const showPayDialog=ref(false)
const payLoading=ref(false)
const currentOrderNo=ref('')

const fundReportList=ref([])//公示列表
const showReportDialog=ref(false)//提交弹窗
const reportForm=reactive({
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
    }catch(error)
    {
        console.error(error)
    }finally{
        payLoading.value=false
    }
}

onMounted(()=>{
    fetchDetail()
    fetchComments()//加载留言
    fetchFundReports()
})


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
    if(!reportForm.reportAmount||reportForm.reportAmount<=0){
        return ElMessage.warning('请输入正确的支出金额')
    }
    if(!reportForm.content.trim()){
        return ElMessage.warning('请填写资金使用说明')
    }

    try{
        reportForm.projectId=project.value.projectId

        await request.post('/fund-report/add',reportForm)
        ElMessage.success('报告提交成功，等待管理员审核')

        //关闭弹窗，清空表单，并刷新列表
        showReportDialog.value=false
        reportForm.reportAmount=100
        reportForm.content=''
        fetchFundReports()
    }catch(e)
    {
        console.error("提交资金报告失败",e)
    }
}

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
.comment-item{
    padding: 15px 0;
    border-bottom: 1px dashed #ebeef5;
}
.comment-item:last-child{
    border-bottom: none;
}
.comment-header{
    display: flex;
    justify-content: space-between;
    margin-bottom: 8px;
}
.comment-header .username{
    font-weight: bold;
    color:#409eff
}
.comment-header .time{
    font-size: 13px;
    color: #909399;
}
.comment-content{
    color:#333;
    line-height: 1.6;
    font-size: 14px;
}
</style>