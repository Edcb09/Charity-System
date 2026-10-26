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

//登录信息在localStorage里的所有key，401时需要一起清掉
const AUTH_KEYS=['satoken','username','role','avatar','userId']
let last401Time=0//同一时刻并发多个请求时，只提示一次

//响应拦截器：统一处理错误
service.interceptors.response.use(
    response=>{
        //文件下载（responseType='blob'）不是 JSON 包装结果，直接把 Blob 交给调用方处理
        if(response.config.responseType==='blob'){
            return response.data
        }
        const res=response.data
        if(res.code !==200)
        {
            if (res.code==401)//未登录 / 登录态已失效
            {
                // 服务端会话可能已经没了（Redis 重启清空、超时过期、换了设备等），
                // 但 localStorage 里还留着旧 token 和用户名，页面会显示"已登录"却处处 401。
                // 这里清掉残留登录信息，让前端状态和服务端保持一致。
                const hadToken=!!localStorage.getItem('satoken')
                AUTH_KEYS.forEach(k=>localStorage.removeItem(k))

                const now=Date.now()
                if(now-last401Time>2000){
                    last401Time=now
                    if(hadToken){
                        // 之前的登录态失效了：刷新一次页面，头部/按钮等状态恢复成游客
                        ElMessage.warning('登录已失效，请重新登录')
                        setTimeout(()=>window.location.reload(),800)
                    }else{
                        ElMessage.warning('请先登录')
                        useUserStore().openLoginDialog()
                    }
                }
            }else{
                ElMessage.error(res.msg||'系统错误')
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
