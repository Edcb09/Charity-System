import { createRouter, createWebHistory } from 'vue-router'
import { ElMessage } from 'element-plus'
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
      { path: 'profile', name: 'Profile', component: () => import('@/views/Profile.vue') },//个人中心
      { path: 'dashboard', name: 'Dashboard', component: () => import('@/views/Dashboard.vue') }//echarts
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
  // 用路由 name 判断，避免 /create-project/12 这类带参路径漏判
  const roleRoutes = {
    CreateProject: '1', // 发起/编辑项目：仅申请人
    MyProjects: '1',    // 我的项目：仅申请人
    MyDonations: '0',   // 我的捐款：仅捐赠人
    Admin: '2',         // 管理员工作台：仅管理员
    Dashboard: '2'      // 数据驾驶舱：仅管理员
  }
  const needRole = roleRoutes[to.name]
  if(needRole !== undefined && role !== needRole){
    ElMessage.warning('无权限访问该页面')
    return '/'
  }

})

export default router
