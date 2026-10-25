<template>
  <div class="dashboard-container" v-loading="loading">
    <!-- 顶部返回区 -->
    <div class="header-bar">
      <el-button @click="$router.push('/')" plain round>
        <el-icon><Back /></el-icon> 返回首页
      </el-button>
      <h2>📊 慈善平台数据</h2>
      <div style="width: 100px;"></div> <!-- 占位保持居中 -->
    </div>

    <!-- 1. 数据卡片区 -->
    <el-row :gutter="20" class="card-row">
      <el-col :span="6">
        <div class="data-card card-green">
          <div class="card-title">累计筹款金额</div>
          <div class="card-value">￥{{ formatAmount(dashboardData.cards?.totalAmount) }}</div>
        </div>
      </el-col>
      <el-col :span="6">
        <div class="data-card card-blue">
          <div class="card-title">累计捐赠人次</div>
          <div class="card-value">{{ dashboardData.cards?.totalDonations || 0 }} 次</div>
        </div>
      </el-col>
      <el-col :span="6">
        <div class="data-card card-orange">
          <div class="card-title">平台项目总数</div>
          <div class="card-value">{{ dashboardData.cards?.totalProjects || 0 }} 个</div>
        </div>
      </el-col>
      <el-col :span="6">
        <div class="data-card card-purple">
          <div class="card-title">平台注册用户</div>
          <div class="card-value">{{ dashboardData.cards?.totalUsers || 0 }} 人</div>
        </div>
      </el-col>
    </el-row>

    <!-- 2. 图表区 -->
    <el-row :gutter="20" class="chart-row">
      <!-- 左侧：近7天折线图 -->
      <el-col :span="14">
        <el-card shadow="hover" class="chart-card">
          <template #header><div class="chart-title">近 7 天筹款趋势</div></template>
          <div ref="trendChartRef" style="height: 350px; width: 100%;"></div>
        </el-card>
      </el-col>
      
      <!-- 右侧：类型占比饼图 -->
      <el-col :span="10">
        <el-card shadow="hover" class="chart-card">
          <template #header><div class="chart-title">项目类型分布</div></template>
          <div ref="pieChartRef" style="height: 350px; width: 100%;"></div>
        </el-card>
      </el-col>
    </el-row>

    <!-- 3. 底部柱状图 -->
    <el-row :gutter="20" style="margin-top: 20px;">
      <el-col :span="24">
        <el-card shadow="hover" class="chart-card">
          <template #header><div class="chart-title">筹款金额 Top 5 项目</div></template>
          <div ref="barChartRef" style="height: 300px; width: 100%;"></div>
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<script setup>
import { ref, onMounted, onBeforeUnmount, nextTick } from 'vue'
import request from '@/utils/request'
import * as echarts from 'echarts'
import { Back } from '@element-plus/icons-vue'

const loading = ref(false)
const dashboardData = ref({})

// DOM 引用
const trendChartRef = ref(null)
const pieChartRef = ref(null)
const barChartRef = ref(null)

// 图表实例
let trendChart = null
let pieChart = null
let barChart = null

const formatAmount = (amount) => {
  if (!amount) return '0.00'
  return Number(amount).toLocaleString('zh-CN', { minimumFractionDigits: 2 })
}

const fetchData = async () => {
  loading.value = true
  try {
    const res = await request.get('/admin/dashboard/data')
    dashboardData.value = res.data
    await nextTick() // 等待 DOM 渲染完
    initCharts()
  } catch (error) {
    console.error(error)
  } finally {
    loading.value = false
  }
}

const initCharts = () => {
  // 1. 折线图 (主题深绿色)
  trendChart = echarts.init(trendChartRef.value)
  trendChart.setOption({
    tooltip: { trigger: 'axis' },
    grid: { left: '3%', right: '4%', bottom: '3%', containLabel: true },
    xAxis: { type: 'category', boundaryGap: false, data: dashboardData.value.trend?.dates || [] },
    yAxis: { type: 'value', name: '金额 (元)' },
    series: [{
      name: '捐款金额',
      type: 'line',
      smooth: true,
      data: dashboardData.value.trend?.amounts || [],
      itemStyle: { color: '#1e6c4f' },
      areaStyle: {
        color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [
          { offset: 0, color: 'rgba(30, 108, 79, 0.5)' },
          { offset: 1, color: 'rgba(30, 108, 79, 0.05)' }
        ])
      }
    }]
  })

  // 2. 饼图 (环形)
  pieChart = echarts.init(pieChartRef.value)
  pieChart.setOption({
    tooltip: { trigger: 'item' },
    legend: { bottom: '5%', left: 'center' },
    series: [{
      name: '项目类型',
      type: 'pie',
      radius: ['40%', '70%'],
      avoidLabelOverlap: false,
      itemStyle: { borderRadius: 10, borderColor: '#fff', borderWidth: 2 },
      label: { show: false, position: 'center' },
      emphasis: {
        label: { show: true, fontSize: 18, fontWeight: 'bold' }
      },
      data: dashboardData.value.projectTypes || []
    }]
  })


    // 3. 柱状图 (横向)
  barChart = echarts.init(barChartRef.value)
  barChart.setOption({
    // 修改 1：删掉原来的 title 组件，不再占用左上角
    tooltip: { trigger: 'axis', axisPointer: { type: 'shadow' } },
    
    // 修改 2：调整 grid 底部边距，给左下角的文字留出空间
    grid: { left: '3%', right: '10%', bottom: '10%', top: '5%', containLabel: true },
    
    // 修改 3：把单位名称交给 xAxis 的 name 属性，并强制靠左 (nameLocation: 'start')
    // 调整底部边距，给下方的文字留出空间
    grid: { left: '3%', right: '10%', bottom: '15%', top: '5%', containLabel: true },
    
    xAxis: { 
      type: 'value',
      name: '已筹金额(元)',
      nameLocation: 'start', // 放在最左侧起点
      nameGap: 60,          
      nameTextStyle: {
        color: '#909399',
        fontSize: 12,
        align: 'left',       // 文字左对齐，会向右伸展
        verticalAlign: 'top',
        padding:[8,0,0,-60]
      }
    },
    
    yAxis: { 
      type: 'category', 
      data: (dashboardData.value.top5?.titles || []).reverse() 
    },
    
    series: [{
      name: '已筹金额',
      type: 'bar',
      barWidth: '50%',
      data: (dashboardData.value.top5?.amounts || []).reverse(),
      itemStyle: {
        color: '#F2994A', 
        borderRadius: [0, 8, 8, 0]
      },
      label: {
        show: true,
        position: 'right',
        color: '#666',
        formatter: '{c}'
      }
    }]
  })
}

// 监听窗口缩放
const handleResize = () => {
  trendChart?.resize()
  pieChart?.resize()
  barChart?.resize()
}

onMounted(() => {
  fetchData()
  window.addEventListener('resize', handleResize)
})

onBeforeUnmount(() => {
  window.removeEventListener('resize', handleResize)
  trendChart?.dispose()
  pieChart?.dispose()
  barChart?.dispose()
})
</script>

<style scoped>
.dashboard-container {
  padding: 20px;
  background-color: #f4f6f8;
  min-height: 100%;
}
.header-bar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
}
.header-bar h2 {
  margin: 0;
  color: #1e6c4f;
  letter-spacing: 2px;
}
.card-row { margin-bottom: 20px; }
.data-card {
  padding: 20px;
  border-radius: 12px;
  color: white;
  box-shadow: 0 4px 12px rgba(0,0,0,0.1);
  transition: transform 0.3s;
}
.data-card:hover { transform: translateY(-5px); }
.card-title { font-size: 14px; opacity: 0.9; margin-bottom: 10px; }
.card-value { font-size: 28px; font-weight: bold; font-family: 'Arial', sans-serif; }

/* 卡片渐变色 */
.card-green { background: linear-gradient(135deg, #11998e 0%, #38ef7d 100%); }
.card-blue { background: linear-gradient(135deg, #2193b0 0%, #6dd5ed 100%); }
.card-orange { background: linear-gradient(135deg, #f12711 0%, #f5af19 100%); }
.card-purple { background: linear-gradient(135deg, #8E2DE2 0%, #4A00E0 100%); }

.chart-card { border-radius: 12px; border: none; }
.chart-title { font-weight: bold; color: #333; border-left: 4px solid #1e6c4f; padding-left: 10px; }
</style>