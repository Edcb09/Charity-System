<template>
    <el-container class="layout-container">
        <el-header class="header">
            <div class="logo" @click="$router.push('/')" style="cursor: pointer;">慈善捐款平台</div>
            <div class="user-info">
                <!--未登录时显示登录按钮-->
                <el-button v-if="!token" type="primary" size="default" @click="userStore.openLoginDialog()">登录 / 注册</el-button>

                <!--已登录显示用户名和退出-->
                <template v-else>
                <el-button type="text" style="color:white;margin-right: 15px;" @click="$router.push('/my-donations')">我的捐赠</el-button>
                <span>欢迎您，{{ username }}</span>
                <el-button type="danger" size="small" @click="logout" style="margin-left:15px;">退出登录</el-button>
                <el-button type="success" size="small" style="margin-right: 15px;" @click="$router.push('/create-project')">发起募捐</el-button>
            </template>
            </div>
        </el-header>

        <el-main>
            <!-- 渲染子路由的页面-->
             <router-view />
        </el-main>
        <!--引入登录弹窗组件-->
        <Login />
    </el-container>
</template>

<script setup>
import {computed, ref} from 'vue'
import {useRouter} from 'vue-router'
import {ElMessage} from 'element-plus'
import { useUserStore } from '@/stores/user'
import Login from '@/views/Login.vue'

const router = useRouter()
const userStore =useUserStore()

//动态获取token
const token=computed(()=>localStorage.getItem('satoken'))
const username=computed(()=>localStorage.getItem('username')||'游客')


const logout=()=>{
    localStorage.removeItem('satoken')
    localStorage.removeItem('username')
    ElMessage.success('已退出登录')
    window.location.reload()
    //强制刷新
    window.location.href='/'
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