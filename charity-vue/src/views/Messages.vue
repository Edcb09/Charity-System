<template>
    <div style="max-width: 800px;margin:20px auto";>
        <el-card>
            <template #header>
                <div style="display: flex;justify-content: space-between;align-items: center;">
                    <span style="font-size: 18px;font-weight: bold;">消息中心</span>
                    <div>
                    <el-button type="primary" size="small" plain @click="readAll" :disabled="messageList.length === 0">全部已读</el-button>
                    <!-- 新增：清空消息 -->
                    <el-button type="danger" size="small" plain @click="clearAll" :disabled="messageList.length === 0" style="margin-left: 10px;">清空消息</el-button>
                    </div>
                </div>
            </template>

            <el-empty v-if="messageList.length===0" description="暂无消息" />
            
            <div v-for="msg in messageList" :key="msg.msgId" class="msg-item" :class="{'unread':msg.isRead===0}">
                <div class="msg-header">
                    <el-tag v-if="msg.type === 1" type="success" size="small">审核通知</el-tag>
                    <el-tag v-else-if="msg.type === 2" type="warning" size="small">捐款通知</el-tag>
                    <el-tag v-else-if="msg.type === 3" type="primary" size="small">资金公示</el-tag>
                    <el-tag v-else-if="msg.type === 4" type="danger" size="small">举报处理</el-tag>
                    <el-tag v-else type="info" size="small">系统通知</el-tag>
                    <span class="time">{{ msg.createTime?msg.createTime.replace('T',' '):'' }}</span>
                </div>
                <div class="msg-content">{{ msg.content }}</div>
                <div class="msg-actions">
                    <el-button v-if="msg.isRead === 0" type="text" size="small" @click="markRead(msg.msgId)">标为已读</el-button>
                    <el-button v-if="msg.type === 1 && msg.linkId" type="text" size="small" @click="goToProject(msg)">查看项目</el-button>
                    
                    <!-- 删除单条消息 -->
                    <el-button type="danger" link size="small" style="margin-left: 10px;" @click="deleteMsg(msg.msgId)">删除</el-button>
                </div>
                </div> 
            <!-- 新增：分页组件 -->
        <div style="display: flex; justify-content: center; margin-top: 20px;" v-if="total > 0">
          <el-pagination
            background
            layout="prev, pager, next"
            :total="total"
            :page-size="pageSize"
            @current-change="handlePageChange"
          />
        </div>
      
        </el-card>
    </div>
</template>


<script setup>
import {ref,onMounted} from 'vue'
import request from '@/utils/request'
import { ElMessage,ElMessageBox } from 'element-plus';
import {useRouter} from 'vue-router'
import { useMessageStore } from '@/stores/message';

const messageList=ref([])
const router=useRouter()

const messageStore=useMessageStore()

const total = ref(0)
const pageNum = ref(1)
const pageSize = ref(10)

const fetchMessage = async () => {
  try {
    const res = await request.get('/message/my', {
      params: {
        pageNum: pageNum.value,
        pageSize: pageSize.value
      }
    })
    // 注意：返回的数据结构变了，records 是当前页的数据，total 是总数
    messageList.value = res.data.records || [] 
    total.value = res.data.total || 0
  } catch (error) {
    console.error(error)
  }
}

//页码切换事件
const handlePageChange = (val) => {
  pageNum.value = val
  fetchMessage()
}

const markRead=async(id)=>
{
    try{
        await request.post(`/message/read/${id}`)
        fetchMessage()
        messageStore.fetchUnreadCount()
    }catch(e)
    {
        console.error(e)
    }
}

const readAll=async()=>
{
    try{
        await request.post('/message/readAll')
        ElMessage.success('全部消息已标为已读')
        fetchMessage()
        messageStore.fetchUnreadCount()
    }catch(e)
    {
        console.error(e)
    }
}

onMounted(()=>{
    fetchMessage()
})

//点击查看项目自动标记已读
const goToProject=async(msg)=>{
    if(msg.isRead===0){
        try{
            await request.post(`/message/read/${msg.msgId}`)
            messageStore.fetchUnreadCount()
        }catch(e)
        {
            console.error("标记已读失败",e)
        }
    }
    //跳转页面
    router.push(`/project/${msg.linkId}`)
}

// 删除单条消息
const deleteMsg = (id) => {
  ElMessageBox.confirm('确定要删除这条消息吗？', '提示', {
    confirmButtonText: '确定',
    cancelButtonText: '取消',
    type: 'warning',
  }).then(async () => {
    try {
      await request.delete(`/message/${id}`)
      ElMessage.success('删除成功')
      // 如果当前页只有这1条消息，且不是第一页，自动退回上一页
      if (messageList.value.length === 1 && pageNum.value > 1) {
        pageNum.value--
      }
      fetchMessages() // 刷新列表
      messageStore.fetchUnreadCount() // 更新顶部红点
    } catch (error) {
      console.error(error)
    }
  }).catch(() => {})
}

// 清空所有消息
const clearAll = () => {
  ElMessageBox.confirm('确定要清空所有消息吗？此操作不可恢复！', '清空确认', {
    confirmButtonText: '确定清空',
    cancelButtonText: '取消',
    type: 'error',
  }).then(async () => {
    try {
      await request.delete('/message/clear')
      ElMessage.success('所有消息已清空')
      pageNum.value = 1 // 回到第一页
      fetchMessages()
      messageStore.fetchUnreadCount()
    } catch (error) {
      console.error(error)
    }
  }).catch(() => {})
}


</script>


<style scoped>
.msg-item{
    padding: 15px;
    border-bottom: 1px solid #ebeef5;
    transition:background-color 0.3s;
}
.msg-item.unread{
    background-color: #f0f9eb;
}
.msg-item:last-child{
    border-bottom: none;
}
.msg-header{
    display: flex;
    justify-content: space-between;
    margin-bottom: 8px;
}
.time{
    font-size: 13px;
    color:#909399
}
.msg-content{
    color:#333;
    font-size: 14px;
    margin-bottom: 10px;
}
.msg-actions{
    text-align: right;
}


</style>