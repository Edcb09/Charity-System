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
      // 注意：/message/my 返回的是分页对象，消息数组在 data.records 里，
      // 直接对 data 调 filter 会报 "filter is not a function"，导致悬浮窗永远显示"暂无未读消息"
      if (unreadCount.value > 0) {
        const listRes = await request.get('/message/my', {
          params: { pageNum: 1, pageSize: 50 } // 多取一些，避免未读消息被已读的挤到第二页而漏显示
        })
        const records = listRes?.data?.records || []
        previewList.value = records.filter(m => m.isRead === 0).slice(0, 5)
      } else {
        previewList.value = []
      }
    } catch (error) {
      console.error("获取未读消息失败", error)
    }
  }

  return { unreadCount, previewList,fetchUnreadCount }
})