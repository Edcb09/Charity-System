<template>
    <div style="max-width: 1000px;margin: 20px auto;">
        <el-page-header @back="$router.push('/')" content="我发起的项目" style="margin-bottom: 20px;" />

        <el-card>
            <el-table :data="myProjectList" stripe v-loading="loading">
                <el-table-column prop="title" label="项目名称" min-width="200"/>
                <el-table-column prop="projectType" label="类型" width="100"/>
                <el-table-column prop="targetAmount" label="目标金额" width="120">
                <template #default="scope">￥{{ scope.row.targetAmount }}</template>
                </el-table-column>

                <el-table-column label="状态" width="120">
                    <template #default="scope">
                        <el-tag v-if="scope.row.status===0" type="info" effect="light">草稿</el-tag>
                        <el-tag v-if="scope.row.status===1" type="warning" effect="light">待审核</el-tag>
                        <el-tag v-if="scope.row.status===2" type="success" effect="light">募捐中</el-tag>
                        <el-tag v-if="scope.row.status===3" type="info" effect="light">已结束</el-tag>
                        <el-tag v-if="scope.row.status === 7" type="danger" effect="light">已下架</el-tag>
                        <el-tooltip
                        v-if="scope.row.status===4"
                        :content="'驳回理由：'+(scope.row.rejectReason||'管理员未填写')"
                        placement="top">
                        <el-tag type="danger" style="cursor:pointer;">已驳回</el-tag>
                        </el-tooltip>
                        <el-tag v-if="scope.row.status===5" type="info" effect="light">已归档</el-tag>
                        <el-tag v-if="scope.row.status === 6" type="warning" effect="light">待审核归档</el-tag>
                    </template>
                </el-table-column>

                <el-table-column label="操作" width="240" fixed="right">
                    <template #default="scope">
                    <!--继续编辑按钮-->
                    <el-button
                    v-if="scope.row.status===0||scope.row.status===4"
                    type="warning"
                    size="small"
                    @click="$router.push(`/create-project/${scope.row.projectId}`)"
                    >继续编辑</el-button>

                    <!--可以去详情页-->
                    <el-button 
                    :type="(scope.row.status === 0 || scope.row.status === 4) ? 'primary' : 'success'"
                    size="small" 
                    @click="$router.push(`project/${scope.row.projectId}`)">
                        查看详情
                    </el-button>

                    <!-- 删除按钮：如果是草稿、驳回、或已下架，显示删除 -->
                    <el-button 
                    v-if="[0, 4, 7].includes(scope.row.status)"
                    type="danger" 
                    size="small" 
                    @click="handleDeleteProject(scope.row.projectId)"
                    >删除</el-button>
                    </template>
                </el-table-column>
            </el-table>
            <!-- 分页组件 -->
            <div style="display: flex; justify-content: center; margin-top: 20px;">
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
import {ref,onMounted}from 'vue'
import request from'@/utils/request'
import { ElMessageBox ,ElMessage} from 'element-plus';

const myProjectList =ref([])
const loading=ref(false)
const pageNum=ref(1)
const pageSize=ref(10)
const total=ref(0)

const fetchMyProjects = async () => {
  loading.value = true
  try {
    // 传入分页参数
    const res = await request.get('/project/my', {
      params: {
        pageNum: pageNum.value,
        pageSize: pageSize.value
      }
    })
    myProjectList.value = res.data.records 
    total.value = res.data.total          
  } catch (error) {
    console.error(error)
  } finally {
    loading.value = false
  }
}

const handlePageChange = (val) => {
  pageNum.value = val
  fetchMyProjects()
}

const handleDeleteProject = (id) => {
  ElMessageBox.confirm('确定要删除该项目吗？已发布过的项目删除后将无法恢复。', '删除确认', {
    confirmButtonText: '确定删除',
    cancelButtonText: '取消',
    type: 'warning',
  }).then(async () => {
    try {
      await request.delete(`/project/${id}`)
      ElMessage.success('项目已删除')
      if (myProjectList.value.length === 1 && pageNum.value > 1) {
        pageNum.value--
      }
      fetchMyProjects() // 刷新列表
    } catch (error) { console.error(error) }
  }).catch(() => {})
}

// //弹出驳回理由
// const showRejectReason=(reason)=>{
//     ElMessageBox.alert(reason||'管理员未填写驳回理由','驳回原因',{
//         confirmButtonText:'我知道了',
//         type:'error',
//     })
// }

onMounted(()=>{
    fetchMyProjects()
})

</script>