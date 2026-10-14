<template>
    <el-container class="layout-container">
        <el-header class="header">
            <div class="logo">慈善捐款平台</div>
            <div class="user-info">
                <span>欢迎您，{{ username }}</span>
                <el-button type="danger" size="small" @click="logout" style="margin-left:15px;">退出登录</el-button>
            </div>
        </el-header>

        <el-main>
            <!-- 渲染子路由的页面-->
             <router-view />
        </el-main>
    </el-container>
</template>

<script setup>
import {ref} from 'vue'
import {useRouter} from 'vue-router'
import {ElMessage} from 'element-plus'

const router = useRouter()
const username=ref(localStorage.getItem('username')||'游客')

const logout=()=>{
    localStorage.removeItem('satoken')
    localStorage.removeItem('username')
    ElMessage.success('已退出登录')
    router.push('/login')
}


</script>

<style scoped>
.layout-container{height:100vh;}
.header{
    display: flex;
    justify-content: space-between;
    align-items: center;
    background-color: #409EFF;
    color: white;
    padding: 0 20px;
}
.logo{font-size:20px;font-weight:bold;}
.user-info{display: flex;align-items: center;}
</style>