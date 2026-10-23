<template>
  <div class="project-list">

        <!-- 最新项目轮播图 -->
    <div v-if="latestProjects.length > 0" class="carousel-container">
      <el-carousel :interval="4000" type="card" height="320px" indicator-position="outside">
        <el-carousel-item v-for="item in latestProjects" :key="item.projectId" @click="goDetail(item.projectId)">
          <div class="carousel-item-box">
            <img 
              :src="item.coverImg || 'https://via.placeholder.com/800x400?text=No+Image'" 
              @error="(e) => e.target.src = 'https://via.placeholder.com/800x400?text=No+Image'"
              class="carousel-img" 
            />
            <div class="carousel-title">
              {{ item.title }}
              <span class="carousel-amount">已筹 ￥{{ item.currentAmount }} / ￥{{ item.targetAmount }}</span>
            </div>
          </div>
        </el-carousel-item>
      </el-carousel>
    </div>

    <!-- 新增：Tab 筛选栏 -->
    <div class="tab-container">
      <div class="tab-item" :class="{ active: currentTab === 'all' }" @click="changeTab('all')">全部</div>
      <div class="tab-item" :class="{ active: currentTab === '2' }" @click="changeTab('2')">募捐中</div>
      <div class="tab-item" :class="{ active: currentTab === '3' }" @click="changeTab('3')">已结束</div>
      <div class="tab-item" :class="{ active: currentTab === '5' }" @click="changeTab('5')">已归档</div>
    </div>
        

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
    const status = route.query.status || '' 
    currentTab.value = status === '' ? 'all' : status

    const res = await request.get('/project/list', {
      params: {
        pageNum: pageNum.value,
        pageSize: pageSize.value,
        keyword: keyword,
        projectType: projectType,
        status: status || null
      }
    })
    projectList.value = res.data.records
    total.value = res.data.total
  } catch (error) {
    console.error(error)
  }
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
    fetchLatestProjects()
})

// 添加 Tab 变量
const currentTab = ref('all')

// 切换 Tab 方法
const changeTab = (tab) => {
  currentTab.value = tab
  pageNum.value = 1 // 切换 Tab 时回到第一页
  
  // 将状态同步到 URL 中
  router.push({
    path: '/',
    query: {
      ...route.query, // 保留搜索框的 keyword 和 type
      status: tab === 'all' ? undefined : tab // 如果是全部，就不传 status
    }
  })
}

// 轮播图变量
const latestProjects = ref([])

// 获取最新5个募捐中的项目
const fetchLatestProjects = async () => {
  try {
    const res = await request.get('/project/list', {
      params: {
        pageNum: 1,
        pageSize: 5,
        status: 2 // 只查募捐中的
      }
    })
    latestProjects.value = res.data.records
  } catch (error) {
    console.error("获取轮播图失败", error)
  }
}


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
/* Tab 栏样式 */
.tab-container {
  display: flex;
  background-color: #fff;
  padding: 10px 20px;
  border-radius: 8px;
  box-shadow: 0 2px 8px rgba(0,0,0,0.05);
  margin-bottom: 10px;
}

.tab-item {
  font-size: 16px;
  color: #666;
  padding: 8px 16px;
  cursor: pointer;
  position: relative;
  transition: all 0.3s;
  font-weight: 500;
}
.tab-item:hover {
  color: var(--el-color-primary);
}
/* 选中状态 */
.tab-item.active {
  color: var(--el-color-primary);
  font-weight: bold;
}
/* 选中状态的底部下划线 */
.tab-item.active::after {
  content: '';
  position: absolute;
  bottom: 0;
  left: 50%;
  transform: translateX(-50%);
  width: 24px;
  height: 3px;
  background-color: var(--el-color-primary);
  border-radius: 2px;
}

/* 轮播图外层容器 */
.carousel-container {
  background-color: #fff;
  padding: 20px 0;
  border-radius: 12px;
  box-shadow: 0 2px 12px rgba(0,0,0,0.04);
  margin-bottom: 20px;
}

/* 单个轮播卡片 */
.carousel-item-box {
  position: relative;
  width: 100%;
  height: 100%;
  cursor: pointer;
  border-radius: 12px;
  overflow: hidden;
}

/* 图片铺满 */
.carousel-img {
  width: 100%;
  height: 100%;
  object-fit: cover;
  display: block;
}

/* 底部渐变黑底和文字 */
.carousel-title {
  position: absolute;
  bottom: 0;
  left: 0;
  width: 100%;
  /* 从透明到黑色的渐变背景，让白字看得清 */
  background: linear-gradient(to top, rgba(0,0,0,0.85) 0%, rgba(0,0,0,0) 100%);
  color: #fff;
  padding: 40px 20px 20px 20px;
  font-size: 18px;
  font-weight: bold;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
  box-sizing: border-box;
}

/* 已筹金额小字 */
.carousel-amount {
  display: block;
  font-size: 13px;
  color: #ffd04b; /* 亮黄色，醒目 */
  font-weight: normal;
  margin-top: 5px;
}
/* 调整 Element Plus 卡片的圆角 */
:deep(.el-carousel__item) {
  border-radius: 12px;
}
/* 轮播图底部指示器（未激活状态：灰色） */
:deep(.el-carousel__indicators--outside .el-carousel__button) {
  background-color: #c0c4cc; /* 浅灰色 */
  opacity: 0.5; /* 稍微带点透明，看起来更柔和 */
  height: 3px; /* 把默认的圆点或者粗条变成细细的线条 */
  width: 24px;
  border-radius: 2px;
}
/* 轮播图底部指示器（当前激活亮起状态：主题深绿色） */
:deep(.el-carousel__indicators--outside .el-carousel__indicator.is-active .el-carousel__button) {
  background-color: var(--el-color-primary); /* 使用全局主题色 */
  opacity: 1; /* 完全不透明 */
}
</style>