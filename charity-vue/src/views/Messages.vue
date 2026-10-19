<template>
    <div style="max-width: 800px;margin:20px auto";>
        <el-card>
            <template #header>
                <div style="display: flex;justify-content: space-between;align-items: center;">
                    <span style="font-size: 18px;font-weight: bold;">消息中心</span>
                    <el-button type="primary" size="small" @click="readAll" :disabled="messageList.length===0">全部已读</el-button>
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
                    <!--如果未读，可以点击标记为已读-->
                    <el-button v-if="msg.isRead===0" type="text" size="small" @click="markRead(msg.msgId)">标记已读</el-button>
                    <!--审核通过的消息可以点击跳转项目详情-->
                    <el-button v-if="msg.type===1&&msg.linkId" type="text" size="small" @click="goToProject(msg)">查看项目</el-button>
                </div>
            </div>
        </el-card>
    </div>
</template>


<script setup>
import {ref,onMounted} from 'vue'
import request from '@/utils/request'
import { ElMessage } from 'element-plus';
import {useRouter} from 'vue-router'
import { useMessageStore } from '@/stores/message';

const messageList=ref([])
const router=useRouter()

const messageStore=useMessageStore()

const fetchMessage=async()=>
{
    try{
        const res=await request.get('/message/my')
        messageList.value=res.data
    }catch(e)
    {
        console.error(e)
    }
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