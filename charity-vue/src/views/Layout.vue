<template>
  <el-container class="layout-container">
    <el-header class="header">
      <div class="logo" @click="$router.push('/')">
        <el-icon :size="24" style="margin-right: 8px;"><Platform /></el-icon>
        慈善捐款平台
      </div>
      
      <!-- 中部搜索栏 -->
      <div class="header-search">
        <el-popover
          placement="bottom"
          :width="400"
          trigger="click"
          popper-class="search-history-popover"
        >
          <!-- 触发源：搜索输入框 -->
          <template #reference>
            <div style="width: 100%;">
              <el-input
                v-model="searchKeyword"
                placeholder="请输入项目名称"
                class="search-input"
                @keyup.enter="handleSearch"
              >
                <template #prepend>
                  <el-select v-model="searchType" placeholder="全部分类" style="width: 110px;" @keyup.enter="handleSearch">
                    <el-option label="全部分类" value="" />
                    <el-option label="助学" value="助学" />
                    <el-option label="医疗" value="医疗" />
                    <el-option label="救灾" value="救灾" />
                    <el-option label="其他" value="其他" />
                  </el-select>
                </template>
                <template #append>
                  <el-button @click="handleSearch">
                    <el-icon><Search /></el-icon>
                  </el-button>
                </template>
              </el-input>
            </div>
          </template>

          <!-- 下拉面板：搜索历史 -->
          <div class="history-panel">
            <div v-if="historyList.length === 0" class="empty-history">
              暂无搜索记录
            </div>
            <div v-else>
              <div 
                v-for="(item, index) in historyList" 
                :key="index" 
                class="history-item"
                @click="selectHistory(item)"
              >
                <span v-if="item.type" class="history-type">[{{ item.type }}]</span>
                <span class="history-keyword">{{ item.keyword }}</span>
              </div>
            </div>
            
            <!-- 底部清除按钮 -->
            <div class="history-footer" v-if="historyList.length > 0">
              <el-button type="danger" link size="small" @click="clearHistory">
                <el-icon><Delete /></el-icon> 清除搜索记录
              </el-button>
            </div>
          </div>
        </el-popover>
      </div>




      <div class="user-info">
        <!-- ==================== 游客状态（未登录） ==================== -->
        <template v-if="!token">
          <!-- 纯文字展示 -->
          <div class="login-text" @click="userStore.openLoginDialog()">登录 / 注册</div>
        </template>

        <!-- ==================== 已登录状态 ==================== -->
        <template v-else>
          <!-- 顶部快捷发起按钮 (仅申请人可见) -->
          <el-button v-if="role === '1'" type="success" size="small" round style="margin-right: 15px;" @click="$router.push({ name: 'CreateProject' })">
            <el-icon><Plus /></el-icon> 发起募捐
          </el-button>

          <el-button
          v-if="role === '2'"
          type="warning"
          size="small"
          round
          style="margin-right: 15px; font-weight: bold;"
          @click="$router.push('/dashboard')"
          >
          <el-icon style="margin-right: 4px;"><DataLine /></el-icon> 数据大屏
          </el-button>

            <!----- 消息红点与悬浮小窗 ----->
          <el-popover 
            v-if="role!=='2'"
            placement="bottom-end"
            :width="320"
            trigger="hover"
            popper-class="msg-popover"
            @show="messageStore.fetchUnreadCount()"
          >
            <!-- 悬浮时显示的内容 -->
            <template #reference>
              <!-- 使用 div 替代 button，方便让 hover 背景撑满上下 -->
              <div class="nav-action-btn" @click="$router.push('/messages')">
                <span>消息通知</span>
                <span
                v-if="messageStore.unreadCount>0"
                class="msg-red-dot">
                {{ messageStore.unreadCount>99?'99+':messageStore.unreadCount }}
                </span>
              </div>
            </template>

            <!-- 悬浮小窗内部渲染 -->
            <div class="preview-box">
              <div class="preview-header">最新未读消息</div>
              
              <el-empty 
                v-if="messageStore.previewList.length === 0" 
                description="暂无未读消息" 
                :image-size="60" 
                style="padding: 10px 0;"
              />
              
              <div v-else class="preview-list">
                <div 
                  v-for="msg in messageStore.previewList" 
                  :key="msg.msgId" 
                  class="preview-item"
                  @click="$router.push('/messages')"
                >
                  <el-tag size="small" :type="getMsgType(msg.type).color" effect="light" style="margin-right: 8px;">
                    {{ getMsgType(msg.type).label }}
                  </el-tag>
                  <div class="preview-content">{{ msg.content }}</div>
                </div>
              </div>

              <div v-if="messageStore.previewList.length > 0" class="preview-footer" @click="$router.push('/messages')">
                查看全部消息 >
              </div>
            </div>
          </el-popover>


          <!-- 用户下拉菜单 -->
          <el-dropdown trigger="click" @command="handleCommand">
            <div class="user-profile">
              <el-avatar :size="32" :src="userAvatar || 'https://cube.elemecdn.com/3/7c/3ea6beec64369c2642b92c6726f1epng.png'" />
              <span style="margin-left: 8px;">{{ username }}</span>
              <el-icon style="margin-left: 5px;"><ArrowDown /></el-icon>
            </div>
            <template #dropdown>
              <el-dropdown-menu>
                <el-dropdown-item command="profile">
                  <el-icon><User /></el-icon> 个人中心
                </el-dropdown-item>
                <el-dropdown-item v-if="role === '0'" command="myDonations">
                  <el-icon><Money /></el-icon> 我的捐款
                </el-dropdown-item>
                <el-dropdown-item v-if="role === '1'" command="myProjects">
                  <el-icon><List /></el-icon> 我的项目
                </el-dropdown-item>
                <el-dropdown-item v-if="role === '2'" command="admin" divided>
                  <el-icon><Setting /></el-icon> 后台审核
                  <el-badge v-if="adminPendingTotal > 0" :value="adminPendingTotal" style="margin-left: 10px;" />
                </el-dropdown-item>
                <el-dropdown-item command="logout" divided>
                  <el-icon><SwitchButton /></el-icon> 退出登录
                </el-dropdown-item>
              </el-dropdown-menu>
            </template>
          </el-dropdown>
        </template>
      </div>
    </el-header>
    
    <el-main class="main-content">
      <router-view v-slot="{ Component }">
        <transition name="fade" mode="out-in">
          <component :is="Component" />
        </transition>
      </router-view>
    </el-main>

    <Login />
    <SmartAssistant />
  </el-container>
</template>

<script setup>
import SmartAssistant from '@/components/SmartAssistant.vue'
import { ref, computed, onMounted, watch } from 'vue'
import { useRoute,useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { useMessageStore } from '@/stores/message'
import { useUserStore } from '@/stores/user' 
import { Platform, Plus, ArrowDown, User, Money, List, Setting, SwitchButton, DataLine  } from '@element-plus/icons-vue'
import request from '@/utils/request'
import Login from '@/views/Login.vue' 
import { Search, Delete } from '@element-plus/icons-vue'

const route=useRoute()
const router = useRouter()
const messageStore = useMessageStore()
const userStore = useUserStore()

const token = computed(() => localStorage.getItem('satoken'))
const role = computed(() => localStorage.getItem('role') || '0')
const username = computed(() => localStorage.getItem('username') || '游客')
const userAvatar = ref(localStorage.getItem('avatar') || '')
const adminPendingTotal = ref(0)

const fetchAdminPendingCount = async () => {
  if (role.value !== '2' || !token.value) return
  try {
    const res = await request.get('/admin/pendingCount')
    adminPendingTotal.value = res.data.total
  } catch (error) { console.error(error) }
}

const handleCommand = (command) => {
  if (command === 'profile') router.push('/profile')
  if (command === 'myDonations') router.push('/my-donations')
  if (command === 'myProjects') router.push('/my-projects')
  if (command === 'admin') router.push('/admin')
  if (command === 'logout') logout()
}

const logout = () => {
  localStorage.clear() 
  ElMessage.success('已退出登录')
  window.location.href = '/' 
}

// 辅助函数：消息类型映射
const getMsgType = (type) => {
  const map = {
    1: { label: '审核', color: 'success' },
    2: { label: '捐款', color: 'warning' },
    3: { label: '资金', color: 'primary' },
    4: { label: '举报', color: 'danger' },
    5: { label: '系统', color: 'info' }
  }
  return map[type] || { label: '系统', color: 'info' }
}

watch(token, (newVal) => {
  if (newVal) {
    userAvatar.value = localStorage.getItem('avatar') || ''
    messageStore.fetchUnreadCount()
    fetchAdminPendingCount()
  }
})

// 搜索相关的变量
const searchKeyword = ref('')
const searchType = ref('')

const historyList=ref([])

// 处理搜索跳转
const handleSearch = () => {
    const keyword = searchKeyword.value.trim()
  // 1. 严格校验：只有下拉框选了合法的值，才取它
  const validTypes = ['助学', '医疗', '救灾', '其他']
  let currentType = validTypes.includes(searchType.value) ? searchType.value : ''

//   // 2. 去重：如果输入框内容和下拉框选的内容一模一样，就清空 type，避免历史记录里出现重复
//   if (keyword === currentType) {
//     currentType = ''
//   }

  // 3. 保存历史记录
  if (keyword || currentType) {
    saveHistory(keyword, currentType)
  }

  // 4. 跳转搜索
  router.push({
    path: '/',
    query: {
      keyword: keyword || undefined,
      type: currentType || undefined,
      status: route.query.status || undefined
    }
  })
}

// 保存历史记录逻辑
const saveHistory = (keyword, type) => {
  if (!keyword && !type) return

  // 👇 合法分类白名单
  const validTypes = ['助学', '医疗', '救灾', '其他']
  // 如果传进来的 type 不在白名单里，直接置为空字符串
  const safeType = validTypes.includes(type) ? type : ''

  // 过滤掉重复的（关键字和分类都相同视为重复）
  let newHistory = historyList.value.filter(
    item => !(item.keyword === keyword && item.type === safeType)
  )
  
  newHistory.unshift({ keyword, type: safeType })
  newHistory = newHistory.slice(0, 10)
  
  historyList.value = newHistory
  localStorage.setItem('searchHistory', JSON.stringify(newHistory))
}

// 点击历史记录项
const selectHistory = (item) => {
  searchKeyword.value = item.keyword || ''
  
  // 👇 校验：只有合法的值才赋给下拉框
  const validTypes = ['助学', '医疗', '救灾', '其他']
  searchType.value = validTypes.includes(item.type) ? item.type : ''
  
  handleSearch() // 触发搜索
}

// 清除历史记录
const clearHistory = () => {
  historyList.value = []
  localStorage.removeItem('searchHistory')
  ElMessage.success('搜索记录已清空')
}

// 当在别的页面点击 Logo 回首页时，清空搜索框
watch(() => route.path, (newPath) => {
  if (newPath !== '/') {
    searchKeyword.value = ''
    searchType.value = ''
  }
})

onMounted(() => {
const savedHistory = localStorage.getItem('searchHistory')
  if (savedHistory) {
    // 👇 读取时，过滤掉以前存错的脏数据
    const validTypes = ['助学', '医疗', '救灾', '其他']
    let parsed = JSON.parse(savedHistory)
    
    historyList.value = parsed.map(item => {
      return {
        keyword: item.keyword,
        type: validTypes.includes(item.type) ? item.type : ''
      }
    }).filter(item => item.keyword || item.type) // 去掉全空的项
    
    // 重新保存清洗后的数据
    localStorage.setItem('searchHistory', JSON.stringify(historyList.value))
  }
  if (token.value) {
    messageStore.fetchUnreadCount()
    fetchAdminPendingCount()
  }
})
</script>

<style scoped>
.layout-container { 
  display: flex;
  flex-direction: column; 
  /* height: 100vh; 
  overflow: hidden;  */
  background-color: #f4f6f8;
  /* 至少铺满一屏：否则内容不足一屏的页面（审核页、个人中心、捐款记录等）
     灰色背景只到内容底部，下方会露出浏览器默认的白色 */
  min-height: 100vh;
}
.header {
  flex-shrink: 0;
  display: flex;
  justify-content: space-between;
  align-items: center;
  height: 60px;
  background: linear-gradient(135deg, #0f4c3a 0%, #1e6c4f 100%);
  color: white;
  padding: 0 30px;
  box-shadow: 0 4px 15px rgba(242, 153, 74, 0.3); 
  z-index: 10;
}
.logo {
  font-size: 22px;
  font-weight: bold;
  cursor: pointer;
  display: flex;
  align-items: center;
  letter-spacing: 1px;
}
.user-info {
  display: flex;
  align-items: center;
  height:100%;
}
.user-profile {
  display: flex;
  align-items: center;
  cursor: pointer;
  color: white;
  padding: 5px 10px;
  border-radius: 20px;
  transition: background 0.3s;
}
.user-profile:hover {
  background: rgba(255,255,255,0.15);
}


/* 登录文字的样式 */
.login-text {
  color: white;
  font-size: 16px;
  font-weight: bold;
  letter-spacing: 1px;
  cursor: pointer;
  padding: 0px 25px;
  height:100%;
  border-radius: 0;
  transition: all 0.3s ease;
  /* 垂直居中 */
  display: flex;
  align-items: center;
  justify-content: center;
}
.login-text:hover {
  background: rgba(255, 255, 255, 0.2); /* 悬停时加一点淡淡的白色背景 */
  text-shadow: 0 0 8px rgba(255,255,255,0.8);
}

.main-content {
  flex: 1;
  padding: 20px 40px;
  /* overflow-y: auto; */
  box-sizing: border-box;
}
.fade-enter-active,
.fade-leave-to {
  transition: opacity 0.3s ease;
}
.fade-enter-from,
.fade-leave-to {
  opacity: 0;
}


.header-search {
  flex: 1; /* 占据中间剩余空间 */
  display: flex;
  justify-content: center;
  max-width: 600px;
  margin: 0 20px;
}
.search-input {
  width: 100%;
}
/* 调整内部输入框的圆角，更美观 */
.search-input :deep(.el-input-group__prepend) {
  background-color: #f5f7fa;
}
.search-input :deep(.el-input__wrapper) {
  border-top-right-radius: 0;
  border-bottom-right-radius: 0;
}
/* 历史记录面板 */
.history-panel {
  padding: 5px 0;
}
.empty-history {
  text-align: center;
  color: #999;
  font-size: 13px;
  padding: 15px 0;
}
.history-item {
  display: flex;
  align-items: center;
  padding: 8px 12px;
  cursor: pointer;
  border-radius: 4px;
  transition: background-color 0.2s;
  font-size: 14px;
}
.history-item:hover {
  background-color: #f5f7fa;
}
.history-type {
  color: var(--el-color-primary); /* 用主题深绿色 */
  font-size: 13px;
  margin-right: 5px;
}
.history-keyword {
  color: #333;
}
.history-footer {
  text-align: right;
  padding-top: 10px;
  margin-top: 5px;
  border-top: 1px solid #ebeef5;
}


/* --- 顶部右侧通用文字按钮（消息通知用） --- */
.nav-action-btn {
  display: flex;
  align-items: center;
  justify-content: center;
  height: 60px; /* 与 el-header 默认高度一致 */
  padding: 0 15px;
  color: white;
  font-size: 15px;
  cursor: pointer;
  transition: background 0.3s;
}
.nav-action-btn:hover {
  background: rgba(255, 255, 255, 0.15); /* 撑满上下的 hover 底色 */
}
/* 覆盖 el-badge 内部的默认下划线等 */
.nav-action-btn :deep(.el-badge__content) {
  border: none;
}

/* 新增：独立的红点样式 */
.msg-red-dot {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  background-color: #F56C6C; /* 红色背景 */
  color: #fff; /* 白色文字 */
  font-size: 12px;
  font-weight: bold;
  height: 18px;
  min-width: 18px;
  padding: 0 6px;
  border-radius: 10px; 
  margin-left: 8px; /* 与“消息通知”文字隔开一点距离 */
  line-height: 1;
  box-shadow: 0 2px 4px rgba(245, 108, 108, 0.3); 
}

/* --- 悬浮小窗内容样式 --- */
.preview-box {
  padding: 5px;
}
.preview-header {
  font-weight: bold;
  font-size: 14px;
  color: #333;
  border-bottom: 1px solid #ebeef5;
  padding-bottom: 10px;
  margin-bottom: 10px;
}
.preview-list {
  max-height: 300px;
  overflow-y: auto;
}
.preview-item {
  display: flex;
  align-items: flex-start;
  padding: 10px 5px;
  border-bottom: 1px dashed #f0f0f0;
  cursor: pointer;
  transition: background 0.2s;
}
.preview-item:hover {
  background-color: #f5f7fa;
}
.preview-item:last-child {
  border-bottom: none;
}
.preview-content {
  font-size: 13px;
  color: #666;
  line-height: 1.4;
  /* 多行文本省略号 */
  display: -webkit-box;
  -webkit-box-orient: vertical;
  -webkit-line-clamp: 2;
  overflow: hidden;
}
.preview-footer {
  text-align: center;
  font-size: 13px;
  color: #409EFF;
  cursor: pointer;
  padding-top: 10px;
  border-top: 1px solid #ebeef5;
  margin-top: 5px;
}
.preview-footer:hover {
  color: #66b1ff;
}

/* 搜索按钮 hover 样式：放大镜变主题色 */
.search-input :deep(.el-input-group__append .el-button:hover) {
  color: var(--el-color-primary) !important; /* 图标变主题深绿色 */
  background-color: #f0f9f5; 
  border-color: var(--el-color-primary); 
}

/* 确保放大镜图标本身也能正确变色（防止 Element Plus 默认样式覆盖） */
.search-input :deep(.el-input-group__append .el-button:hover .el-icon) {
  color: var(--el-color-primary);
  font-weight: bold; 
  transition: all 0.3s; 
}
</style>