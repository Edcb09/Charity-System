<template>
<div class="home">
    <h1>慈善系统首页</h1>
    <p>登陆成功，欢迎使用系统</p>
    <el-button @click="logout">退出登录</el-button>
</div>
</template>

<script setup>
import {useRouter} from 'vue-router'
const router=useRouter()

const logout=()=>{
    localStorage.removeItem('satoken')
    router.push('/login')
}
</script>

<style scoped>
.home{
    padding:30px;
}
</style>