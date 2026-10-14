import axios from 'axios'
import { ElMessage} from "element-plus";
import router from '@/router'
import  {useUserStore}  from '@/stores/user';

const service=axios.create(
    {
        baseURL: '/api' ,//自动加上/api前缀
        timeout: 5000
    }
)

//请求拦截器：自动携带Token
service.interceptors.request.use(config=>
{
    const token=localStorage.getItem('satoken')
    if(token)
    {
        config.headers['satoken']=token//后端Sa-Token默认从satoken请求头读取
    }
    return config
})

//响应拦截器：统一处理错误
service.interceptors.response.use(
    response=>{
        const res=response.data
        if(res.code !==200)
        {
            ElMessage.error(res.msg||'系统错误')
            if (res.code==401)//未登录
            {
                const userStore=useUserStore()
                userStore.openLoginDialog()
            }
            return Promise.reject(new Error(res.msg||'Error'))
        }
        return res
    },
    error=>{
        ElMessage.error(error.message||'网络错误')
        return Promise.reject(error)
    }
)
export default service