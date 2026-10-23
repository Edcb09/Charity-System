<template>
    <div style="max-width: 800px;margin: 20px auto;">
        <el-card>
            <template #header>
                <div class="card-header">
                    <span>发起慈善募捐项目</span>
                    <el-button @click="$router.push('/')">返回首页</el-button>
                </div>
            </template>

            <el-form :model="form" label-width="120px">
                <el-form-item label="募捐项目标题" required>
                    <el-input v-model="form.title" placeholder="请输入项目标题" />
                </el-form-item>

                <el-form-item label="项目类型" required>
                    <el-select v-model="form.projectType" placeholder="请选择类型">
                        <el-option label="助学" value="助学" />
                        <el-option label="医疗" value="医疗" />
                        <el-option label="救灾" value="救灾" />
                        <el-option label="其他" value="其他" />
                    </el-select>
                </el-form-item>

                <el-form-item label="目标金额（元）" required>
                    <el-input-number v-model="form.targetAmount" :min="1":precision="2" />
                </el-form-item>

                <el-form-item label="截止时间" required>
                    <el-date-picker
                    v-model="form.endTime"
                    type="datetime"
                    placeholder="选择截止日期"
                    value-format="YYYY-MM-DD[T]HH:mm:ss"
                    :disabled-date="disabledDate"
                    />
                </el-form-item>

        <el-form-item label="项目封面图">
          <el-upload
            class="uploader-box"
            :show-file-list="false"
            :http-request="(options) => uploadImage(options, 'cover')"
            :before-upload="beforeUpload"
          >
            <img v-if="form.coverImg" :src="form.coverImg" class="uploaded-img" />
            <el-icon v-else class="uploader-icon"><Plus /></el-icon>
          </el-upload>
          <div style="font-size: 12px; color: #999; margin-left: 10px;">建议尺寸 800x400</div>
        </el-form-item>

        <el-form-item label="证明材料图片">
          <el-upload
            class="uploader-box"
            :show-file-list="false"
            :http-request="(options) => uploadImage(options, 'proof')"
            :before-upload="beforeUpload"
          >
            <img v-if="form.proofImg" :src="form.proofImg" class="uploaded-img" />
            <el-icon v-else class="uploader-icon"><Plus /></el-icon>
          </el-upload>
          <div style="font-size: 12px; color: #999; margin-left: 10px;">上传病历、证明信等</div>
        </el-form-item>

                <el-form-item label="受助人信息" required>
                    <el-input v-model="form.recipientInfo" type="textarea" :rows="2" placeholder="受助人基本信息" />
                </el-form-item>

                    <el-form-item label="项目详细描述" required>
                        <el-input v-model="form.description" type="textarea" :rows="5" placeholder="请输入项目描述..." />
                    </el-form-item>


                    <el-form-item>
                     <!-- v-if 判断：被驳回(4)的状态下，隐藏“存为草稿”按钮，防止admin记录同步-->
                     <el-button 
                       v-if="!isEdit || form.status === 0" 
                       @click="submitProject(0)"
                     >存为草稿</el-button>
                     <el-button type="primary" @click="submitProject(1)">提交审核</el-button>
                    </el-form-item>

               
            </el-form>
        </el-card>
    </div>

</template>


<script setup>
import{onMounted, reactive}from 'vue'
import { useRoute, useRouter } from 'vue-router';
import request from '@/utils/request'
import { ElMessage } from 'element-plus';
import { Plus } from '@element-plus/icons-vue'

const router=useRouter()
const route=useRoute()

const projectId=route.params.id
const isEdit=!!projectId

const form=reactive({
    projectId:null,
    title:'',
    projectType:'',
    targetAmount:1000,
    endTime:'',
    coverImg:'',
    recipientInfo:'',
    description:'',
    status:0//默认为草稿
})

onMounted(async()=>{
    if(isEdit){
        try{
            const res=await request.get(`/project/${projectId}`)
            Object.assign(form,res.data)
        }catch(e){
            ElMessage.error('获取项目详情失败')
        }
    }
})

const submitProject = async (statusCode) => {
  //拦截：如果是驳回状态，强制提交审核
  if (isEdit && form.status === 4) {
    statusCode = 1
  }

  if (statusCode === 0 && !form.title) {
    return ElMessage.warning('请至少填写项目标题后再保存草稿')
  }
  if (statusCode === 1) {
    if (!form.title || !form.projectType || !form.targetAmount || !form.endTime || !form.recipientInfo || !form.description) {
      return ElMessage.warning('提交审核需填写所有必填项')
    }
  }

    const endTimeDate = new Date(form.endTime).getTime()
    const nowDate = new Date().getTime()
    if (endTimeDate <= nowDate) {
      return ElMessage.warning('项目的截止时间不能早于或等于当前时间，请重新选择！')
    }
  

  form.status = statusCode 

  try {
    let res
    if (isEdit) {
      res = await request.put('/project/update', form) 
    } else {
      res = await request.post('/project/add', form) 
    }
    ElMessage.success(res.msg)
    router.push('/my-projects') 
  } catch (error) {
    console.error(error)
  }
}

// 新建项目上传图片前校验
const beforeUpload = (file) => {
  const isImage = file.type === 'image/jpeg' || file.type === 'image/png' || file.type === 'image/jpg'
  const isLt2M = file.size / 1024 / 1024 < 2
  if (!isImage) {
    ElMessage.error('只能上传 JPG/PNG 格式的图片!')
  }
  if (!isLt2M) {
    ElMessage.error('图片大小不能超过 2MB!')
  }
  return isImage && isLt2M
}

// 自定义上传方法
const uploadImage = async (options, type) => {
  const formData = new FormData()
  formData.append('file', options.file)
  
  try {
    const res = await request.post('/file/upload', formData, {
      headers: {
        'Content-Type': 'multipart/form-data',
        'satoken': localStorage.getItem('satoken')
      }
    })
    // res.data 是后端返回的图片 URL
    if (type === 'cover') {
      form.coverImg = res.data
    } else if (type === 'proof') {
      form.proofImg = res.data
    }
    ElMessage.success('图片上传成功')
  } catch (error) {
    console.error(error)
    ElMessage.error('图片上传失败')
  }
}

// 禁用设置过去
const disabledDate = (time) => {
  // time 是一个 Date 对象
  return time.getTime() < Date.now()
}

</script>

<style scoped>
.card-header{
    display: flex;
    justify-content: space-between;
    align-items: center;
}

/* 上传框的样式 */
.uploader-box :deep(.el-upload) {
  border: 1px dashed #d9d9d9;
  border-radius: 8px;
  cursor: pointer;
  position: relative;
  overflow: hidden;
  transition: var(--el-transition-duration-fast);
  width: 160px;
  height: 90px;
  display: flex;
  justify-content: center;
  align-items: center;
  background-color: #fafafa;
}

.uploader-box :deep(.el-upload:hover) {
  border-color: var(--el-color-primary); /* 悬停时变成主题深绿色 */
}

.uploader-icon {
  font-size: 28px;
  color: #8c939d;
  width: 160px;
  height: 90px;
  text-align: center;
  line-height: 90px;
}

/* 上传成功后展示的图片 */
.uploaded-img {
  width: 160px;
  height: 90px;
  object-fit: cover;
  display: block;
}
</style>