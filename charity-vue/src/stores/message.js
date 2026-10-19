import { defineStore } from 'pinia'
import { ref } from 'vue'
import request from '@/utils/request'

export const useMessageStore = defineStore('message', () => {
  const unreadCount = ref(0)

  // 从后端获取最新的未读数量
  const fetchUnreadCount = async () => {
    if (!localStorage.getItem('satoken')) return
    try {
      const res = await request.get('/message/unreadCount')
      unreadCount.value = res.data
    } catch (error) {
      console.error("获取未读消息失败", error)
    }
  }

  return { unreadCount, fetchUnreadCount }
})