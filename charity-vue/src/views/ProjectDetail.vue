<template>
    <div class="=deatail-container" v-if="project.projectId">
        <el-card>
            <el-page-header @back="$router.push('/')" content="募捐详情" style="margin-bottom: 20px;" />
            <el-row :gutter="30">
                <!--左侧图片与信息-->
                <el-col :span="10">
                <img :src="project.coverImg || 'https://via.placeholder.com/500x300' "class="detail-img" />
                </el-col>

                <!--右侧核心信息-->
                <el-col :span="14">
                    <h1>{{ project.title }}</h1>
                    <p class="type-tag">项目类型：{{ ProjectList.projectType }}</p>

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

                    <el-divider />

                    <div class="action-box">
                        <el-input-number v-model="donateAmount" :min="1" :max="999999" label="捐款金额" />
                        <el-checkbox v-model="isAnonymous" style="margin-left: 20px;">匿名捐款</el-checkbox>
                        <el-button type="danger" size="large" style="width: 100%;margin-top: 20px;"@click="handleDonate">
                            立即捐款
                        </el-button>
                    </div>
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
                <el-button type="primary" @click="confirmPay" :loading="payLoading">请确认支付</el-button>
            </template>
        </el-dialog>
    </div>

</template>

<script setup>
import {ref,onMounted,computed} from 'vue'
import {useRoute,useRouter} from 'vue-router'
import request from '@/utils/request'
import { ElMessage } from 'element-plus';
import { useUserStore } from '@/stores/user';
import { P } from 'vue-router/dist/index-u4XkZVDG.js';

const route=useRoute()
const router=useRouter()
const userStore=useUserStore()

const project=ref({})
const donateAmount =ref(10)
const isAnonymous =ref(false)
const showPayDialog=ref(false)
const payLoading=ref(false)
const currentOrderNo=ref('')

//计算进度
const calculatePercentage=computed(()=>{
    if(!project.value.targetAmount) return 0
    let p=(project.value.currentAmount/project.value.targetAmount)*100
    return p>100?100:Math.floor(p)
})

//获取详情
const fetchDetail =async()=>{
    try{
        const res=await request.get('/project/${route.params.id}')
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
        //1.创建捐款
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
        await request.post('donation/pay',{orderNo:currentOrderNo.value})
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
})


</script>

<style scoped>
.detail-container{max-width: 1000px;margin: 20px auto;}
.detail-img{width: 100%;border-radius: 8px;object-fit: cover;}
h1{font-size: 24px;margin-bottom: 10px;}
.type-tag{color: #909299;font-size: 14px;background: #f4f4f5;display: inline-block;padding: 2px 8px;border-radius: 4px;}
.amount-box{display: flex;justify-content: space-between;background: #f8f9fa;padding: 15px;border-radius: 8px;margin-bottom: 20px;}
.label{display: block;font-size: 14px;color: #666;margin-bottom: 5px;}
.value{font-size: 20px;font-weight: bold;}
.highlight{color:"#f56c6c"}
.description{line-height: 1.8;color: #555;text-indent: 2em;}

</style>