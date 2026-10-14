<template>
    <div class="project-list">
        <!--搜索与筛选栏-->
        <el-card class="search-bar">
            <el-input v-model="keyword" placeholder="搜索慈善项目" style="width: 200px;margin-right: 10px;" />
                <el-select v-model="projectType" placeholder="慈善项目类型" clearable style="width: 150px; margin-right: 10px;">
                <el-option label="助学" value="助学"/>
                <el-option label="医疗" value="医疗"/>
                <el-option label="救灾" value="救灾"/>
                </el-select>
                <el-button type="primary" @click="fetchData">搜索</el-button>
        </el-card>

        <!--慈善项目卡片展示-->
        <el-row :gutter="20" style="margin-top: 20px;">
            <el-col :span="8" v-for="item in projectList" :key="item.projectId" style="margin-bottom: 20px;">
                <el-card class="project-card" :body-style="{padding:'0px'}" shadow="hover">
                    <img :src="item.coverImg || 'https://via.placeholder.com/300x150'" class="cover-img" />
                    <div style="padding: 14px;">
                        <h3 style="white-space: nowrap;overflow:hidden;text-overflow: ellipsis; margin-bottom: 10px;" :title="item.title">{{ item.title }}</h3>
                        <p style="color: #666;font-size: 13px;">类型：{{ item.projectType }}</p>
                        <el-progress :percentage="calculatePercentage(item.currentAmount,item.targetAmount)" />
                        <div style="margin-top: 10px; font-size: 13px; color:#666">
                            <div style="margin-bottom: 5px;">
                                已筹集：<span style="color:#F56C6C;font-weight: bold;">￥{{ item.currentAmount }}</span>
                            </div>
                            <div>
                                目标金额：<span style="color: #409eff;font-weight: bold;">￥{{ item.targetAmount }}</span>
                            </div>
                        </div>
                        <el-button type="primary" size="small" style="width: 100%;margin-top: 15px;" @click="goDetail(item.projectId)">查看详情</el-button>
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
import {ref,onMounted} from 'vue'
import {useRouter} from 'vue-router'
import request from '@/utils/request'

const router=useRouter()
const projectList=ref([])
const total=ref(0)
const pageNum=ref(1)
const pageSize=ref(9)
const keyword=ref('')
const projectType=ref('')


//计算进度条百分比
const calculatePercentage=(current,target)=>{
    if(!target||target===0)return 0
    const percent=(current/target)*100
    return percent>100?100:Math.floor(percent)
}

//获取数据
const fetchData =async()=>{
    try{
        const res=await request.get('/project/list',{
            params:{
                pageNum: pageNum.value,
                pageSize: pageSize.value,
                keyword: keyword.value,
                projectType: projectType.value
            }

        })
        projectList.value=res.data.records
        total.value=res.data.total
    }catch(error)
    {
        console.error(error)
    }
}


//分页切换
const handlePageChange=(val)=>{
    pageNum.value =val
    fetchData()
}

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
.search-bar{display: flex;align-items: center;}
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