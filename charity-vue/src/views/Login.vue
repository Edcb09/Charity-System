<template>
    <el-dialog 
    v-model="userStore.showLoginDialog"
    title="登录"
    width="400px"
    :close-on-click-modal="true"
    :close-on-press-escape="true"
    append-to-body
    @closed="resetForm"
    >
            <el-form class="login-form" :model="form" label-width="45px" @submit.prevent>

            <el-form-item label="账号">
                <el-input v-model="form.username" placeholder="请输入账号" @keyup.enter="handleLogin" />
            </el-form-item>

            <el-form-item label="密码">
                <el-input v-model="form.password" type="password" placeholder="请输入密码" show-password @keyup.enter="handleLogin"/>
            </el-form-item>

            <el-form-item>
                <el-button type="primary" size="large" @click="handleLogin" style="width:100%;margin-top: 10px;font-size: 16px;">登录</el-button>
            </el-form-item>

            </el-form>

            <div class="register-link">
                还没有账号？<el-button type="primary" link @click="goToRegister">去注册</el-button>
            </div>

    </el-dialog>
</template>

<script setup>
import { reactive } from 'vue';
import { useRouter } from 'vue-router';
import request from '@/utils/request';
import { ElMessage } from 'element-plus';
import { useUserStore } from '@/stores/user' 

const userStore = useUserStore()

const router = useRouter();
const form = reactive({
    username: '',
    password: ''
});

const handleLogin= async() =>{
    if(!form.username||!form.password)
{
    return ElMessage.warning('请输入账号和密码');
}
try{
    const res=await request.post('/user/login',form)
    localStorage.setItem('satoken',res.data.token)//存Token
    localStorage.setItem('username',res.data.username)//存用户名
    localStorage.setItem('role',res.data.role)//存入角色
    localStorage.setItem('avatar', res.data.avatar || '')//存入头像
    localStorage.setItem('userId', res.data.userId)
    ElMessage.success('登录成功');
    userStore.closeLoginDialog()
    router.push('/')//跳转首页
    window.location.reload()
}catch(error){
    console.error(error);
}
}

const goToRegister=()=>{
    userStore.closeLoginDialog()
    router.push('/register')
}


//关闭弹窗时重置表单
const resetForm=()=>{
    form.username=''
    form.password=''
}

</script>

<style scoped>

.register-link{
    text-align:right;
    font-size:14px;
    color: #666;
    padding-right: 10px;
    margin-top: 10px;
    border-top: 1px solid #eee;
    padding-top: 15px;
}

.login-form{
    width: 320px;
    margin: 30px auto 0;
}

:deep(.el-dialog__header .el-dialog__title)
{
  font-size: 20px;
  font-weight: bold;
  color: #0f4c3a; 
  letter-spacing: 1px;
}

:deep(.el-input__wrapper.is-focus) {
  box-shadow: 0 0 0 1px var(--el-color-primary) inset !important;
}

</style>