<template>
  <div class="register-container">
    <el-card class="register-card">
      <h2 class="title">注册慈善捐款系统</h2>
      
      <el-form 
      ref="formRef"
      :model="form" 
      :rules="rule"
      label-width="80px" 
      @submit.prevent>
        <el-form-item label="账号" required>
          <el-input v-model="form.username" placeholder="请输入登录账号" />
        </el-form-item>
        
        <el-form-item label="密码" required>
          <el-input v-model="form.password" type="password" show-password placeholder="请输入密码" />
        </el-form-item>

        <el-form-item label="确认密码" required>
          <el-input v-model="confirmPassword" type="password" show-password placeholder="请再次输入密码" />
        </el-form-item>

        <el-form-item label="真实姓名">
          <el-input v-model="form.realName" placeholder="选填" />
        </el-form-item>

        <el-form-item label="手机号">
          <el-input v-model="form.phone" placeholder="选填" />
        </el-form-item>

        <el-form-item label="注册身份" required>
          <el-radio-group v-model="form.role">
            <el-radio :label="0">捐赠人（可捐款、留言）</el-radio>
            <el-radio :label="1">项目申请人（可发起募捐）</el-radio>
          </el-radio-group>
        </el-form-item>

        <el-form-item>
          <el-button type="primary" @click="handleRegister" style="width: 100%;">注 册</el-button>
        </el-form-item>
      </el-form>

      <div class="footer-link">
        已有账号？<router-link to="/">去登录</router-link>
      </div>
    </el-card>
  </div>
</template>

<script setup>
import { reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import request from '@/utils/request'
import { ElMessage } from 'element-plus'
import { useUserStore } from '@/stores/user'

const router = useRouter()
const userStore = useUserStore()

const form = reactive({
  username: '',
  password: '',
  realName: '',
  phone: '',
  role: 0 // 默认捐赠人
})

const confirmPassword = ref('')

const handleRegister = async () => {
  // 基础校验
  if (!form.username || !form.password || !confirmPassword.value) {
    return ElMessage.warning('请填写账号和密码')
  }
  if (form.password !== confirmPassword.value) {
    return ElMessage.warning('两次输入的密码不一致')
  }

  if (!formRef.value) return
  await formRef.value.validate(async (valid) => {
    if (valid) {
      try {
        const res = await request.post('/user/register', form)
        ElMessage.success('注册成功！请登录')
        router.push('/')
      } catch (error) {
        console.error(error)
      }
    } else {
      return false
    }
  })
}

const formRef=ref(null)
//表单校验规则
const rules = {
  username: [
    { required: true, message: '请输入账号', trigger: 'blur' },
    { min: 3, max: 20, message: '账号长度应在 3 到 20 个字符之间', trigger: 'blur' }
  ],
  password: [
    { required: true, message: '请输入密码', trigger: 'blur' },
    { min: 6, max: 20, message: '密码长度应在 6 到 20 个字符之间', trigger: 'blur' }
  ],
  phone: [
    // 手机号选填，但如果填了就必须符合正则
    { pattern: /^1[3-9]\d{9}$/, message: '请输入正确的11位手机号', trigger: 'blur' }
  ],
  role: [
    { required: true, message: '请选择注册身份', trigger: 'change' }
  ]
}
</script>

<style scoped>
.register-container {
  display: flex;
  justify-content: center;
  align-items: center;
  height: 100vh;
  background-color: #f0f2f5;
}
.register-card {
  width: 450px;
  padding: 20px;
  border-radius: 12px;
}
.title {
  text-align: center;
  margin-bottom: 30px;
  color: #333;
}
.footer-link {
  text-align: right;
  font-size: 14px;
  margin-top: 10px;
}
.footer-link a {
  color: #409EFF;
  text-decoration: none;
}
</style>