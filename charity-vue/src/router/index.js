import { createRouter, createWebHistory } from 'vue-router'
import Login from '@/views/Login.vue'
import Layout from '@/views/Layout.vue'
import ProjectList from '@/views/ProjectList.vue'

const router = createRouter({
  history: createWebHistory(import.meta.env.BASE_URL),
  routes: [
    {path:'/login',name:'Login',component:Login},
    {path:'/',
      component:Layout,
      children:[
        {path:'',name:'ProjectList',component:ProjectList}//默认展示项目列表
      ]
    }
  ],
})

//路由守卫：未登录强制跳转登录页面
router.beforeEach((to,from,next)=>{
  const token=localStorage.getItem('satoken')
  if(to.path!=='/login'&&to.path!=='/register'&&!token){
    next('/login')
  }else{
    next()
  }
})

export default router
