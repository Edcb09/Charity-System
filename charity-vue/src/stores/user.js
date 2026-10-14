import {defineStore} from 'pinia'
import {ref} from 'vue'

export const useUserStore=defineStore('user',()=>{
    //控制登录弹窗显示、隐藏
    const showLoginDialog =ref(false)

    //打开弹窗
    const openLoginDialog=()=>{
        showLoginDialog.value=true
    }

    //关闭弹窗
    const closeLoginDialog=()=>{
        showLoginDialog.value=false
    }
    return {showLoginDialog,openLoginDialog,closeLoginDialog}
})