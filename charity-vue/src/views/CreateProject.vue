<template>
    <div style="max-width: 800px;margin: 20px auto;">
        <el-card>
            <template #header>
                <div class="card-header">
                    <span>发起慈善募捐项目</span>
                    <el-button @click="$router.push('/')">返回首页</el-button>
                </div>
            </template>

            <el-form :model="form" label-width="120px">
                <el-form-item label="募捐项目标题" required>
                    <el-input v-model="form.title" placeholder="请输入项目标题" />
                </el-form-item>

                <el-form-item label="项目类型" required>
                    <el-select v-model="form.projectType" placeholder="请选择类型">
                        <el-option label="助学" value="助学" />
                        <el-option label="医疗" value="医疗" />
                        <el-option label="救灾" value="救灾" />
                        <el-option label="其他" value="其他" />
                    </el-select>
                </el-form-item>

                <el-form-item label="目标金额（元）" required>
                    <el-input-number v-model="form.targetAmount" :min="1":precision="2" />
                </el-form-item>

                <el-form-item label="截止时间" required>
                    <el-date-picker
                    v-model="form.endTime"
                    type="datetime"
                    placeholder="选择截止日期"
                    value-format="YYYY-MM-DD[T]HH:mm:ss"
                    />
                </el-form-item>

                <el-form-item label="封面图片链接">
                    <el-input v-model="form.coverImg" placeholder="请输入图片URL(测试用)" />
                </el-form-item>

                <el-form-item label="受助人信息" required>
                    <el-input v-model="form.recipientInfo" type="textarea" :rows="2" placeholder="受助人基本信息" />
                </el-form-item>

                    <el-form-item label="项目详细描述" required>
                        <el-input v-model="form.description" type="textarea" :rows="5" placeholder="请输入项目描述..." />
                    </el-form-item>

                <el-form-item>
                    <el-button type="primary" @click="submitProject">提交审核</el-button>
                </el-form-item>
               
            </el-form>
        </el-card>
    </div>

</template>


<script setup>
import{reactive}from 'vue'
import { useRouter } from 'vue-router';
import request from '@/utils/request'
import { ElMessage } from 'element-plus';

const router=useRouter()
const form=reactive({
    title:'',
    projectType:'',
    targetAmount:1000,
    endTime:'',
    coverImg:'',
    recipientInfo:'',
    description:''
})


const submitProject=async()=>
{
    //校验是否登录
    const token=localStorage.getItem('satoken')
    if(!token){
        ElMessage.warning('登录已失效，请重新登录后再提交')
        return
    }


    if(!form.title||!form.projectType||!form.targetAmount||!form.endTime||!form.recipientInfo||!form.description){
        return ElMessage.warning('请填写所有必填项')
    }

    try{
        const res=await request.post('/project/add',form)
        ElMessage.success(res.msg)
        router.push('/')
    }catch(error){
        console.error(error)
    }
}


</script>

<style scoped>
.card-header{
    display: flex;
    justify-content: space-between;
    align-items: center;
}

</style>