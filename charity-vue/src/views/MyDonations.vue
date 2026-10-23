<template>
    <div style="max-width: 1000px;margin: 20px auto;">
        <el-page-header @back="$router.push('/')" content="我的捐款记录" style="margin-bottom: 20px;" />
        <el-table :data="tableData" style="width: 100%" script v-loading="loading">
            <el-table-column prop="donateTime" label="捐款时间" width="180" >
                <template #default="scope">
                    <span>{{ scope.row.donateTime?scope.row.donateTime.split('T')[0]:'-' }}</span>
                </template>
            </el-table-column>
            <el-table-column prop="projectTitle" label="捐赠项目" />
            <el-table-column prop="amount" label="捐赠金额" width="120">
                <template #default="scope">
                    <span style="color:#f56c6c ;font-weight: bold;">￥{{ scope.row.amount}}</span>
                </template>
            </el-table-column>
            <el-table-column prop="isAnonymous" label="是否匿名" width="100">
                <template #default="scope">
                    <el-tag :type="scope.row.isAnonymous === 1 ? 'info' : 'success'">
                        {{ scope.row.isAnonymous === 1 ? '匿名' : '实名' }}
                    </el-tag>
                </template>
            </el-table-column>
                    
        <el-table-column label="操作" width="150" fixed="right">
          <template #default="scope">
            <el-button type="warning" size="small" plain @click="downloadCertificate(scope.row)">下载证书</el-button>
          </template>
        </el-table-column>
        </el-table>

        <el-pagination
        background layout="prev,pager,next"
        :total="total" :page-size="pageSize"
        @current-change="handlePageChange"
        style="margin-top:20px;justify-content: center;"
        />

            <!-- 👇 隐藏的证书模板，利用绝对定位移出屏幕外，保证截图清晰但不影响页面显示 -->
    <div style="position: fixed; left: -9999px; top: 0;">
      <div id="certificate-node" class="certificate-box">
        <div class="cert-title">捐 赠 证 书</div>
        <div class="cert-subtitle">CERTIFICATE OF DONATION</div>
        
        <div class="cert-content">
          兹证明 <span class="cert-username">{{ certData.username }}</span> 先生/女士，
          <br><br>
          于 <span class="cert-date">{{ certData.date }}</span>，
          通过本平台向 <span class="cert-project">{{ certData.project }}</span> 项目，
          慷慨捐赠人民币 <span class="cert-amount">￥{{ certData.amount }}</span> 元。
          <br><br>
          您的善举将为他人带去温暖与希望，感谢您对慈善事业的鼎力支持！
        </div>
        
        <!-- 底部印章区 -->
        <div class="cert-footer">
          <div class="cert-org">慈善捐款平台</div>
          <div class="cert-stamp">爱心<br>专属章</div>
          <div class="cert-no">证书编号：{{ certData.orderNo }}</div>
        </div>
      </div>
    </div>
    </div>

</template>

<script setup>
import {ref,onMounted,reactive} from 'vue'
import request from '@/utils/request'
import { ElMessage } from 'element-plus'
import html2canvas from 'html2canvas'


const tableData =ref([])
const total=ref(0)
const pageNum=ref(1)
const pageSize=ref(10)
const loading=ref(false)

const fetchData=async()=>
{
    loading.value=true
    try{
        const res=await request.get('/donation/myList',{
            params:{pageNum:pageNum.value,pageSize:pageSize.value}
        })
        tableData.value=res.data.records
        total.value=res.data.total
    }catch(e)
    {
        console.error(e)
    }finally{
        loading.value=false
    }
}

const handlePageChange=(val)=>{
    pageNum.value=val
    fetchData()
}

// 准备用于存放当前要生成证书的数据
const certData = reactive({
  username: '',
  date: '',
  project: '',
  amount: '',
  orderNo: ''
})

// 下载证书方法
const downloadCertificate = async (row) => {
  // 1. 把当前行的数据塞进证书里
  certData.username = localStorage.getItem('username') || '爱心人士'
  // 格式化日期，只要年月日
  certData.date = row.donateTime ? row.donateTime.split('T')[0] : ''
  certData.project = row.projectTitle || '慈善项目'
  certData.amount = row.amount
  // 截取订单号前8位作为证书编号
  certData.orderNo = row.orderNo ? row.orderNo.substring(0, 8).toUpperCase() : 'UNKNOWN'

  // 2. 等待 DOM 更新（确保数据渲染到隐藏的证书里）
  await new Promise(resolve => setTimeout(resolve, 100))

  // 3. 获取证书的 DOM 节点并截图
  const node = document.getElementById('certificate-node')
  if (!node) return ElMessage.error('证书模板加载失败')

  try {
    ElMessage.info('证书生成中，请稍候...')
    const canvas = await html2canvas(node, {
      scale: 2, // 提高清晰度
      backgroundColor: '#ffffff', // 确保背景是白色的
      useCORS: true // 允许加载跨域图片（如果背景有图片的话）
    })

    // 4. 将 Canvas 转为图片链接并触发下载
    const imgUrl = canvas.toDataURL('image/png')
    const link = document.createElement('a')
    link.href = imgUrl
    link.download = `捐赠证书_${certData.project}_${certData.date}.png`
    document.body.appendChild(link)
    link.click()
    document.body.removeChild(link)
    
    ElMessage.success('证书已下载！')
  } catch (error) {
    console.error(error)
    ElMessage.error('证书生成失败')
  }
}

onMounted(()=>fetchData())

</script>

<style scoped>
/* ================= 证书样式 ================= */
.certificate-box {
  width: 800px;
  height: 560px;
  background-color: #fffdf8; /* 微微发黄的宣纸底色 */
  padding: 40px;
  box-sizing: border-box;
  position: relative;
  border: 10px solid #d4af37; /* 金色外边框 */
  outline: 2px solid #d4af37; /* 金色细线 */
  outline-offset: -20px; /* 双边框效果 */
  text-align: center;
  font-family: "STKaiti", "KaiTi", "Microsoft YaHei", sans-serif; /* 优先楷体，更有底蕴 */
}

.cert-title {
  font-size: 42px;
  color: #b8860b; /* 深金色 */
  font-weight: bold;
  letter-spacing: 8px;
  margin-top: 10px;
}

.cert-subtitle {
  font-size: 14px;
  color: #c2a366;
  letter-spacing: 4px;
  margin-bottom: 30px;
  text-transform: uppercase;
}

.cert-content {
  font-size: 20px;
  color: #333;
  line-height: 2;
  padding: 0 40px;
  text-align: left;
  text-indent: 2em;
}

.cert-username {
  font-size: 24px;
  font-weight: bold;
  border-bottom: 2px solid #333;
  padding: 0 10px;
}

.cert-project {
  font-size: 22px;
  font-weight: bold;
}

.cert-amount {
  font-size: 26px;
  color: #d32f2f;
  font-weight: bold;
  font-family: "Microsoft YaHei", sans-serif;
}

.cert-footer {
  position: absolute;
  bottom: 50px;
  right: 60px;
  text-align: right;
}

.cert-org {
  font-size: 20px;
  font-weight: bold;
  margin-bottom: 10px;
  color: #333;
}

.cert-no {
  font-size: 12px;
  color: #888;
  margin-top: 10px;
}

/* 模拟红色圆印章 */
.cert-stamp {
  position: absolute;
  right: 20px;
  bottom: 10px;
  width: 90px;
  height: 90px;
  border: 3px solid #d32f2f;
  border-radius: 50%;
  color: #d32f2f;
  font-size: 16px;
  font-weight: bold;
  display: flex;
  align-items: center;
  justify-content: center;
  text-align: center;
  transform: rotate(-15deg); /* 稍微倾斜，更像真实盖的章 */
  opacity: 0.8;
  line-height: 1.2;
  font-family: "Microsoft YaHei", sans-serif;
}


</style>