<template>
    <div class="project-list">
        

        <!--慈善项目卡片展示-->
        <el-row :gutter="20" style="margin-top: 20px;">
            <el-col :span="8" v-for="item in projectList" :key="item.projectId" style="margin-bottom: 20px;">
                <el-card class="project-card" :body-style="{padding:'0px'}" shadow="hover">
                    <img :src="item.coverImg || 'https://via.placeholder.com/300x150'" class="cover-img" />
                    <div style="padding: 14px;">

                        <!-- <h3 style="white-space: nowrap;overflow:hidden;text-overflow: ellipsis; margin-bottom: 10px;" :title="item.title">{{ item.title }}</h3>
                        <p style="color: #666;font-size: 13px;">类型：{{ item.projectType }}</p> -->

                        <!--增加状态标签和动态按钮-->
                        <div style="display:flex;justify-content: space-between;align-items: center;">
                            <h3 style="margin:0;white-space: nowrap;overflow: hidden;text-overflow: ellipsis;">{{ item.title }}</h3>
                        
                        <el-tag v-if="item.status===2" type="success" effect="dark" size="small">
                            募捐中
                        </el-tag>
                        <el-tag v-else-if="item.status===3" type="info" effect="plain" size="small">已结束</el-tag>
                        <el-tag v-else-if="item.status===5" type="warning" effect="plain" size="small">已归档</el-tag>
                        <el-tag v-if="item.status === 6" type="warning" effect="plain" size="small">待归档</el-tag>

                        </div>
                        <p style="color:#666;font-size:13px;margin-top: 8px;">类型：{{ item.projectType }}</p>
                    


                        <el-progress :percentage="calculatePercentage(item.currentAmount,item.targetAmount)" />
                        <div style="margin-top: 10px; font-size: 13px; color:#666">
                            <div style="margin-bottom: 5px;">
                                已筹集：<span style="color:#F56C6C;font-weight: bold;">￥{{ item.currentAmount }}</span>
                            </div>
                            <div>
                                目标金额：<span style="color: #409eff;font-weight: bold;">￥{{ item.targetAmount }}</span>
                            </div>
                        </div>
                        <el-button 
                        :type="item.status===2?'primary':'info'"
                         size="small" style="width: 100%;margin-top: 15px;" @click="goDetail(item.projectId)">{{  item.status===2?'查看详情':'查看公示结果'}}</el-button>
                    </div>
                </el-card>
            </el-col>
        </el-row>

        <!--分页-->
        <el-pagination
        background
        layout="prev,pager,next"
        :total="total"
        :page-size="pageSize"
        @current-change="handlePageChange"
        style="margin-top: 20px;justify-content: center;"
        />
    </div>
</template>

<script setup>
import {ref,onMounted,watch} from 'vue'
import {useRouter,useRoute} from 'vue-router'
import request from '@/utils/request'

const route=useRoute()
const router=useRouter()
const projectList=ref([])
const total=ref(0)
const pageNum=ref(1)
const pageSize=ref(9)

//计算进度条百分比
const calculatePercentage=(current,target)=>{
    if(!target||target===0)return 0
    const percent=(current/target)*100
    return percent>100?100:Math.floor(percent)
}

// 获取数据的方法
const fetchData = async () => {
  try {
    // 从路由的 query 中获取搜索条件
    const keyword = route.query.keyword || ''
    const projectType = route.query.type || ''

    const res = await request.get('/project/list', {
      params: {
        pageNum: pageNum.value,
        pageSize: pageSize.value,
        keyword: keyword,
        projectType: projectType
      }
    })
    projectList.value = res.data.records
    total.value = res.data.total
  } catch (error) {
    console.error(error)
  }
}

//搜索：重置页码再查询
const onSearch = ()=>{
    pageNum.value =1
    fetchData()
}

//分页切换
const handlePageChange=(val)=>{
    pageNum.value =val
    fetchData()
}


// 监听路由 query 变化，如果发生变化，重新请求数据并重置页码
watch(
  () => route.query,
  () => {
    pageNum.value = 1 // 每次新搜索，都回到第一页
    fetchData()
  },
  { deep: true } // 深度监听
)

//查看详情
const goDetail=(id)=>{
    router.push(`/project/${id}`)
}

//页面加载时获取数据
onMounted(()=>{
    fetchData()
})

</script>

<style scoped>
.cover-img{width: 100%;height: 160px;object-fit: cover;}
.project-card{
    height: 380px;
    display: flex;
    flex-direction: column;
}
.project-card :deep(.el-card__body)
{
    height: 100%;
    display: flex;
    flex-direction: column;
}
.project-card :deep(.el-card__body) > div{
    display: flex;
    flex-direction: column;
    flex: 1;
    overflow: auto;
}
.project-card :deep(.el-button)
{
    margin-top: auto !important; /*强制按钮固定在底部*/
}
.project-card :deep(.el-card__body) > div::-webkit-scrollbar{
  display:none;
}
</style>