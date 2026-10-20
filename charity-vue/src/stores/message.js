import { defineStore } from 'pinia'
import { ref } from 'vue'
import request from '@/utils/request'

export const useMessageStore = defineStore('message', () => {
  const unreadCount = ref(0)
  const previewList=ref([])

  // 从后端获取最新的未读数量
  const fetchUnreadCount = async () => {
    if (!localStorage.getItem('satoken')) return
    try {
      // 1. 获取数量
      const countRes = await request.get('/message/unreadCount')
      unreadCount.value = countRes.data

      // 2. 如果有未读消息，拉取列表并截取前 5 条
      if (unreadCount.value > 0) {
        const listRes = await request.get('/message/my')
        // 过滤出未读，取前5条
        const unreadMsgs = listRes.data.filter(m => m.isRead === 0)
        previewList.value = unreadMsgs.slice(0, 5)
      } else {
        previewList.value = []
      }
    } catch (error) {
      console.error("获取未读消息失败", error)
    }
  }

  return { unreadCount, previewList,fetchUnreadCount }
})