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
                        <el-tag v-if="scope.row.status===0" type="info">草稿</el-tag>
                        <el-tag v-if="scope.row.status===1" type="warning">待审核</el-tag>
                        <el-tag v-if="scope.row.status===2" type="success">募捐中</el-tag>
                        <el-tag v-if="scope.row.status===3" type="info">已结束</el-tag>
                        <el-tag v-if="scope.row.status===4" type="danger">已驳回</el-tag>
                        <el-tag v-if="scope.row.status===5" type="info">已归档</el-tag>
                    </template>
                </el-table-column>

                <el-table-column label="操作" width="200" fixed="right">
                    <template #default="scope">
                        <!--只有被驳回的项目才显示查看驳回理由的按钮-->
                        <el-button
                        v-if="scope.row.status===4"
                        type="danger"
                        size="small"
                        @click="showRejectReason(scope.row.rejectReason)"
                        >查看驳回理由
                    </el-button>
                    <!--可以去详情页-->
                    <el-button type="primary" size="small" @click="$router.push(`project/${scope.row.projectId}`)">
                        查看详情
                    </el-button>
                    </template>
                </el-table-column>
            </el-table>
        </el-card>
    </div>
</template>


<script setup>
import {ref,onMounted}from 'vue'
import request from'@/utils/request'
import { ElMessageBox } from 'element-plus';

const myProjectList =ref([])
const loading=ref(false)

const fetchMyProjects=async()=>{
    loading.value=true
    try{
        const res=await request.get('/project/my')
        myProjectList.value=res.data
    }catch(e)
    {
        console.error(e)
    }finally{
        loading.value=false
    }
}

//弹出驳回理由
const showRejectReason=(reason)=>{
    ElMessageBox.alert(reason||'管理员未填写驳回理由','驳回原因',{
        confirmButtonText:'我知道了',
        type:'error',
    })
}

onMounted(()=>{
    fetchMyProjects()
})

</script>