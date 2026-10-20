import { createRouter, createWebHistory } from 'vue-router'
import Login from '@/views/Login.vue'
import Layout from '@/views/Layout.vue'
import ProjectList from '@/views/ProjectList.vue'
import ProjectDetail from '@/views/ProjectDetail.vue'
import Register from '@/views/Register.vue'

const router = createRouter({
  history: createWebHistory(import.meta.env.BASE_URL),
  routes: [
    {path:'/login',name:'Login',component:Login},
    {path:'/register',name:'Register', component: Register},
    {path:'/',
      component:Layout,
      children:[
      {path:'',name:'ProjectList',component:ProjectList},//默认展示项目列表
      {path:'project/:id',name:'ProjectDetail',component:ProjectDetail},//项目详情
      {path:'my-donations',name:"MyDonations",component:()=>import('@/views/MyDonations.vue')},//查看个人捐赠
      {path:'create-project/:id?',name:'CreateProject',component:()=>import('@/views/CreateProject.vue')},//申请项目
      {path:'admin',name:'Admin',component:()=>import('@/views/Admin.vue')},//管理员审核页面
      {path:'my-projects',name:'MyProjects',component:()=>import('@/views/MyProjects.vue')},//“我”的项目页面
      {path:'messages',name:'Messages',component:()=>import('@/views/Messages.vue')},//消息通知页面
      { path: 'profile', name: 'Profile', component: () => import('@/views/Profile.vue') }//个人中心
      ]
    }
  ],
})

//路由守卫：未登录强制跳转登录页面->修改登录为弹窗形式，本来的拦截已注释
// router.beforeEach((to,from,next)=>{
//   //const token=localStorage.getItem('satoken')
//   //if(to.path!=='/login'&&to.path!=='/register'&&!token){
//   //  next('/login')
//   //}else{
//     next()
//   //}
// })

router.beforeEach((to,from)=>{
  const token=localStorage.getItem('satoken')
  const role=localStorage.getItem('role')

  const publicPaths=['/','/login','/register']
  const isPublicPage=publicPaths.includes(to.path)||to.path.startsWith('/project/')

  if(isPublicPage){
    return true
  }

  if(!token){
    return '/'
  }

  //角色权限路由拦截
  if(to.path==='/create-project'&&role!=='1'){
    return '/';
  }
  if(to.path==='/my-projects'&&role!=='1'){
    return '/'
  }
  if(to.path==='/my-donations'&&role!=='0'){
    return '/'
  }
  if(to.path==='/admin'&&role!=='2'){
    return '/'
  }

})

export default router
