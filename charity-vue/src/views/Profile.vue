<template>
  <div style="max-width: 600px; margin: 20px auto;">
    <el-card>
      <template #header>
        <div class="card-header">
          <span>个人中心</span>
          <el-button @click="$router.push('/')">返回首页</el-button>
        </div>
      </template>

      <div style="text-align: center; margin-bottom: 20px;">
        <!-- 头像上传组件 -->
        <el-upload
          class="avatar-uploader"
          action="/api/file/upload"
          :show-file-list="false"
          :on-success="handleAvatarSuccess"
          :before-upload="beforeAvatarUpload"
          :headers="uploadHeaders"
        >
          <img v-if="form.avatar" :src="form.avatar" class="avatar" />
          <el-icon v-else class="avatar-uploader-icon"><Plus /></el-icon>
        </el-upload>
        <p style="color: #999; font-size: 13px; margin-top: 10px;">点击上传/更换头像</p>
      </div>

      <el-form :model="form" label-width="80px">
        <el-form-item label="账号">
          <el-input v-model="form.username" disabled />
        </el-form-item>
        <el-form-item label="邮箱">
          <el-input v-model="form.email" placeholder="请输入邮箱" />
        </el-form-item>
        <el-form-item label="真实姓名">
          <el-input v-model="form.realName" placeholder="请输入真实姓名" />
        </el-form-item>
        <el-form-item label="手机号">
          <el-input v-model="form.phone" placeholder="请输入手机号" />
        </el-form-item>
        
        <el-form-item>
          <el-button type="primary" @click="saveInfo">保存修改</el-button>
        </el-form-item>
      </el-form>
    </el-card>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { Plus } from '@element-plus/icons-vue' // 需要安装：npm install @element-plus/icons-vue
import request from '@/utils/request'
import { ElMessage } from 'element-plus'

const form = reactive({
  username: '',
  avatar: '',
  email: '',
  realName: '',
  phone: ''
})

// 上传请求需要携带 Token
const uploadHeaders = {
  satoken: localStorage.getItem('satoken')
}

// 获取当前用户信息 (这里为了方便，假设你可以从后端获取自己信息，或者用已有的接口)
// 如果你后端没有获取当前用户详情的接口，我们直接通过 userId 查
onMounted(async () => {
  // 假设我们在登录时缓存了更多信息，如果没有，需要调个接口去查
  // 为了演示，先这样赋值（你后续可以让后端返回整个 User 对象）
  form.username = localStorage.getItem('username')
  
  // 发起请求获取最新的自己信息
  try {
    const res = await request.get('/user/myInfo') // ⚠️ 你需要去后端加这个接口
    Object.assign(form, res.data)
  } catch (error) {
    console.error(error)
  }
})

// 头像上传成功回调
const handleAvatarSuccess = (res) => {
  if (res.code === 200) {
    form.avatar = res.data // 后端返回的图片 URL
    ElMessage.success('头像上传成功')
  } else {
    ElMessage.error(res.msg)
  }
}

// 上传前校验
const beforeAvatarUpload = (file) => {
  const isImage = file.type === 'image/jpeg' || file.type === 'image/png' || file.type === 'image/jpg'
  const isLt2M = file.size / 1024 / 1024 < 2
  if (!isImage) {
    ElMessage.error('头像只能是 JPG/PNG 格式!')
  }
  if (!isLt2M) {
    ElMessage.error('头像大小不能超过 2MB!')
  }
  return isImage && isLt2M
}

// 保存信息
const saveInfo = async () => {
  try {
    await request.post('/user/updateInfo', form)
    ElMessage.success('信息更新成功')
    // 更新本地缓存
    localStorage.setItem('avatar', form.avatar)
    localStorage.setItem('username', form.username)
    // 刷新一下页面
    window.location.reload() 
  } catch (error) {
    console.error(error)
  }
}
</script>

<style scoped>
.card-header { display: flex; justify-content: space-between; align-items: center; }
.avatar-uploader .el-upload {
  border: 1px dashed #d9d9d9;
  border-radius: 50%;
  cursor: pointer;
  position: relative;
  overflow: hidden;
  transition: var(--el-transition-duration-fast);
  width: 100px;
  height: 100px;
  display: flex;
  justify-content: center;
  align-items: center;
}
.avatar-uploader .el-upload:hover {
  border-color: #409EFF;
}
.avatar {
  width: 100px;
  height: 100px;
  object-fit: cover;
  border-radius: 50%;
}
.avatar-uploader-icon {
  font-size: 28px;
  color: #8c939d;
  width: 100px;
  height: 100px;
  line-height: 100px;
  text-align: center;
}
</style>