<template>
    <div style="max-width: 1000px;margin: 20px auto;">
        <el-page-header @back="$router.push('/')" content="我的捐款记录" style="margin-bottom: 20px;" />
        <el-table :data="tableData" style="width: 100%" script v-lodaing="loading">
            <el-table-column prop="donateTime" label="捐款时间" width="180" >
                <template #default="scope">
                    <span>{{ scope.row.donateTime?scope.row.donateTime.split('T')[0]:'-' }}</span>
                </template>
            </el-table-column>
            <el-table-column prop="projectTitle" label="捐赠项目" />
            <el-table-column prop="amount" label="捐赠金额" width="120">
                <template #default="scope">
                    <span style="color:#f56c6c ;font-weight: bold;">￥{{ scope.row.amount}}</span>
                </template>
            </el-table-column>
            <el-table-column prop="isAnonymous" label="是否匿名" width="100">
                <template #default="scope">
                    <el-tag :type="scope.row.isAnonymous === 1 ? 'info' : 'success'">
                        {{ scope.row.isAnonymous === 1 ? '匿名' : '实名' }}
                    </el-tag>
                </template>
            </el-table-column>
        </el-table>

        <el-pagination
        background layout="prev,pager,next"
        :total="total" :page-size="pageSize"
        @current-change="handlePageChange"
        style="margin-top:20px;justify-content: center;"
        />
    </div>

</template>

<script setup>
import {ref,onMounted} from 'vue'
import request from '@/utils/request'

const tableData =ref([])
const total=ref(0)
const pageNum=ref(1)
const pageSize=ref(10)
const loading=ref(false)

const fetchData=async()=>
{
    loading.value=true
    try{
        const res=await request.get('/donation/myList',{
            params:{pageNum:pageNum.value,pageSize:pageSize.value}
        })
        tableData.value=res.data.records
        total.value=res.data.total
    }catch(e)
    {
        console.error(e)
    }finally{
        loading.value=false
    }
}

const handlePageChange=(val)=>{
    pageNum.value=val
    fetchData()
}

onMounted(()=>fetchData())

</script>