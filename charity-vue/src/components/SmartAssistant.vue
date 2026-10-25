<template>
  <div class="assistant-container">
    <!-- 右下角悬浮按钮 -->
    <div class="float-btn" @click="toggleChat" v-show="!isOpen">
      <el-icon :size="24"><Service /></el-icon>
    </div>

    <!-- 聊天窗口 -->
    <el-card class="chat-window" v-show="isOpen" shadow="always">
      <template #header>
        <div class="chat-header">
          <span>💬 系统问答助手</span>
          <el-icon class="close-icon" @click="toggleChat"><Close /></el-icon>
        </div>
      </template>
      
      <div class="chat-body" ref="chatBodyRef">
        <!-- 欢迎语（进入即显示） -->
        <div class="message-item bot">
          <div class="avatar">🤖</div>
          <div class="bubble">您好！我是平台的问答助手。您可以问我关于“捐款”、“发起募捐”、“资金公示”、“审核”等相关问题。</div>
        </div>

        <!-- 聊天记录循环 -->
        <div v-for="(msg, index) in chatHistory" :key="index" class="message-item" :class="msg.role">
          <div class="avatar">{{ msg.role === 'user' ? '👤' : '🤖' }}</div>
          <div class="bubble" v-html="msg.content"></div>
        </div>
      </div>

      <div class="chat-footer">
        <el-input 
          v-model="inputText" 
          placeholder="输入您的问题..." 
          @keyup.enter="sendMessage"
          clearable
        >
          <template #append>
            <el-button @click="sendMessage" :disabled="!inputText.trim()">发送</el-button>
          </template>
        </el-input>
      </div>
    </el-card>
  </div>
</template>

<script setup>
import { ref, nextTick } from 'vue'
import { Service, Close } from '@element-plus/icons-vue'

const isOpen = ref(false)
const inputText = ref('')
const chatHistory = ref([])
const chatBodyRef = ref(null)

// 切换窗口显示/隐藏
const toggleChat = () => {
  isOpen.value = !isOpen.value
  if (isOpen.value) scrollToBottom()
}

// 自动滚动到最新消息
const scrollToBottom = async () => {
  await nextTick()
  if (chatBodyRef.value) {
    chatBodyRef.value.scrollTop = chatBodyRef.value.scrollHeight
  }
}

// ================= 核心：本地知识库 =================
// 只要用户输入中包含 keywords 里的任意词汇，就会触发 answer
const knowledgeBase = [
  {
    keywords: ['捐款', '捐钱', '支付', '怎么捐'],
    answer: '您好！您只需在首页点击感兴趣的项目进入详情页，输入捐款金额并点击“立即捐款”，在弹出的模拟支付窗口中确认支付即可。支持匿名捐款哦！'
  },
  {
    keywords: ['发起', '申请项目', '募捐', '建项目'],
    answer: '如果您是项目申请人身份，登录后点击顶部导航栏的“发起募捐”，填写项目标题、目标金额、受助人信息并上传证明材料。提交后等待管理员审核即可。请耐心等待两到三个工作日~'
  },
  {
    keywords: ['资金','资金公示', '结余', '支出', '报告', '发票'],
    answer: '项目结束（达标或到期）后，申请人需在详情页提交“资金使用报告”并上传发票或凭证。管理员审核通过后，所有人均可在项目详情页的“资金使用公示”板块查看。只有结余为 0 时才能申请结项归档。'
  },
  {
    keywords: ['审核'],
    answer: '您提交慈善项目申请后，平台管理员会在后台进行审核，审核结果（通过 / 驳回）会通知到您，请耐心等待两到三个工作日~您可以在个人中心 - 我的项目页面，查看项目当前状态，审核结果会以消息通知推送。'
  },
 {
    keywords: ['驳回'],
    answer: '项目被驳回一般是材料不全、描述信息不实或者不符合平台慈善项目规范，您可以查看通知里的驳回理由，修改内容后重新提交审核。'
  },
  {
    keywords: ['下架'],
    answer: '当项目存在违规内容、信息造假或者资金使用不合规时，平台会执行下架操作，您可以查看站内通知了解下架原因。'
  },
  {
    keywords: ['实名', '匿名', '名字'],
    answer: '您在捐款时可以勾选“匿名捐款”。勾选后，在项目详情页的“爱心榜”中，您的名字将显示为“爱心人士”，但后台依然会记录您的真实账号，以保证资金流向透明。在捐赠证书中还是会显示您的账号名哦~'
  },
  {
    keywords: ['退款', '退钱'],
    answer: '非常抱歉，由于系统目前为模拟演示环境，暂不支持自助退款。如有特殊情况，请联系管理员协助处理。'
  },
  {
    keywords: ['你好', '你是谁', '在吗'],
    answer: '你好！我是慈善捐款平台的问答助手，有什么可以帮您的吗？'
  }
]

// 模拟打字机效果
const typeWriter = (text) => {
  const msgIndex = chatHistory.value.length
  chatHistory.value.push({ role: 'bot', content: '' })
  
  let i = 0
  const timer = setInterval(() => {
    if (i < text.length) {
      chatHistory.value[msgIndex].content += text.charAt(i)
      i++
      scrollToBottom()
    } else {
      clearInterval(timer)
    }
  }, 30) // 每 30 毫秒输出一个字，模拟真实打字
}

// 发送消息
const sendMessage = () => {
  const text = inputText.value.trim()
  if (!text) return

  // 1. 显示用户消息
  chatHistory.value.push({ role: 'user', content: text })
  inputText.value = ''
  scrollToBottom()

  // 2. 假装系统思考 500ms，然后去知识库里匹配
  setTimeout(() => {
    let matched = false
    for (const item of knowledgeBase) {
      // 只要用户输入的句子中包含了任何一个关键词，就匹配成功
      if (item.keywords.some(k => text.includes(k))) {
        typeWriter(item.answer)
        matched = true
        break // 匹配到就退出循环
      }
    }
    
    // 如果所有关键词都没匹配上
    if (!matched) {
      typeWriter('抱歉，我目前只懂得回答系统操作相关的问题。您可以尝试问我：“怎么捐款？”、“如何发起项目？”或者“资金公示怎么看？”')
    }
  }, 500) 
}
</script>

<style scoped>
/* 悬浮按钮与容器 */
.assistant-container { position: fixed; bottom: 40px; right: 40px; z-index: 9999; }
.float-btn {
  width: 60px; height: 60px;
  background: linear-gradient(135deg, #0f4c3a 0%, #1e6c4f 100%);
  border-radius: 50%;
  display: flex; justify-content: center; align-items: center;
  color: white; cursor: pointer;
  box-shadow: 0 4px 15px rgba(30, 108, 79, 0.4);
  transition: transform 0.3s;
}
.float-btn:hover { transform: scale(1.1); }

/* 聊天窗口 */
.chat-window {
  width: 360px; height: 520px;
  display: flex; flex-direction: column;
  border-radius: 12px; overflow: hidden;
}
:deep(.el-card__header) { padding: 12px 15px; background: #f4f6f8; }
:deep(.el-card__body) { padding: 0; flex: 1; display: flex; flex-direction: column; overflow: hidden; }

.chat-header { display: flex; justify-content: space-between; align-items: center; font-weight: bold; color: #1e6c4f; }
.close-icon { cursor: pointer; color: #999; }
.close-icon:hover { color: #d32f2f; }

/* 聊天主体 */
.chat-body { flex: 1; padding: 15px; overflow-y: auto; background-color: #f9f9f9; }
.message-item { display: flex; margin-bottom: 15px; align-items: flex-start; }
.message-item.user { flex-direction: row-reverse; }

.avatar {
  width: 32px; height: 32px; border-radius: 50%; background: #fff;
  display: flex; align-items: center; justify-content: center;
  font-size: 18px; box-shadow: 0 2px 5px rgba(0,0,0,0.1); flex-shrink: 0;
}

.bubble { max-width: 75%; padding: 10px 14px; border-radius: 8px; font-size: 14px; line-height: 1.5; word-wrap: break-word; }
.message-item.bot .bubble { background: #fff; color: #333; margin-left: 10px; border: 1px solid #ebeef5; }
.message-item.user .bubble { background: #1e6c4f; color: #fff; margin-right: 10px; }

/* 底部输入框 */
.chat-footer { padding: 10px; background: #fff; border-top: 1px solid #ebeef5; }
</style>