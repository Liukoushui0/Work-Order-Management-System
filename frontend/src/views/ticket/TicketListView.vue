<script setup>
import { onMounted, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'

import { fetchTickets } from '@/api/ticket'
import StatusTag from '@/components/StatusTag.vue'
import { useAuthStore } from '@/stores/auth'
import { TICKET_PRIORITY_OPTIONS, TICKET_STATUS_OPTIONS, formatDateTime } from '@/utils/format'
import { normalizeError } from '@/utils/request'

const router = useRouter()
const authStore = useAuthStore()
const loading = ref(false)
const records = ref([])
const total = ref(0)
const dateRange = ref([])

const query = reactive({
  keyword: '',
  status: '',
  priority: '',
  overdue: false,
  pageNum: 1,
  pageSize: 10
})

async function load() {
  loading.value = true
  try {
    const params = { ...query }
    if (dateRange.value && dateRange.value.length === 2) {
      params.startDate = dateRange.value[0]
      params.endDate = dateRange.value[1]
    }
    if (!params.overdue) delete params.overdue
    const data = await fetchTickets(params)
    records.value = data.records || []
    total.value = data.total || 0
  } catch (error) {
    ElMessage.error(normalizeError(error).message)
    records.value = []
    total.value = 0
  } finally {
    loading.value = false
  }
}

function search() {
  query.pageNum = 1
  load()
}

function reset() {
  query.keyword = ''
  query.status = ''
  query.priority = ''
  query.overdue = false
  dateRange.value = []
  search()
}

function openDetail(row) {
  router.push(`/tickets/${row.id}`)
}

onMounted(load)
</script>

<template>
  <div class="page">
    <el-card shadow="never" class="filter-card">
      <el-form :inline="true" @submit.prevent>
        <el-form-item label="关键词">
          <el-input v-model="query.keyword" placeholder="工单编号 / 标题 / 报修人" clearable style="width: 220px"
                    @keyup.enter="search" />
        </el-form-item>
        <el-form-item label="状态">
          <el-select v-model="query.status" placeholder="全部状态" clearable style="width: 140px">
            <el-option v-for="item in TICKET_STATUS_OPTIONS" :key="item.value" :label="item.label"
                       :value="item.value" />
          </el-select>
        </el-form-item>
        <el-form-item label="优先级">
          <el-select v-model="query.priority" placeholder="全部优先级" clearable style="width: 130px">
            <el-option v-for="item in TICKET_PRIORITY_OPTIONS" :key="item.value" :label="item.label"
                       :value="item.value" />
          </el-select>
        </el-form-item>
        <el-form-item label="创建时间">
          <el-date-picker v-model="dateRange" type="daterange" value-format="YYYY-MM-DD"
                          start-placeholder="开始日期" end-placeholder="结束日期" style="width: 240px" />
        </el-form-item>
        <el-form-item>
          <el-checkbox v-model="query.overdue">仅看逾期</el-checkbox>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="search">查询</el-button>
          <el-button @click="reset">重置</el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <el-card shadow="never">
      <template #header>
        <div class="card-header">
          <span>工单列表（共 {{ total }} 条）</span>
          <el-button v-if="authStore.hasPermission('ticket:create')" type="primary" @click="router.push('/tickets/create')">
            <el-icon><Plus /></el-icon>创建工单
          </el-button>
        </div>
      </template>

      <el-table v-loading="loading" :data="records" stripe>
        <el-table-column prop="ticketNo" label="工单编号" width="160" />
        <el-table-column prop="title" label="标题" min-width="120" show-overflow-tooltip />
        <el-table-column prop="customerName" label="客户" min-width="125" show-overflow-tooltip />
        <el-table-column prop="deviceName" label="设备" min-width="115" show-overflow-tooltip />
        <el-table-column label="优先级" width="80">
          <template #default="{ row }">
            <StatusTag kind="priority" :value="row.priority" />
          </template>
        </el-table-column>
        <el-table-column label="状态" width="90">
          <template #default="{ row }">
            <StatusTag :value="row.status" />
          </template>
        </el-table-column>
        <el-table-column prop="assigneeName" label="负责人" width="120">
          <template #default="{ row }">{{ row.assigneeName || '未分派' }}</template>
        </el-table-column>
        <el-table-column label="截止时间" width="170">
          <template #default="{ row }">
            <div class="deadline-cell">
              <span>{{ formatDateTime(row.deadlineTime) }}</span>
              <el-tag v-if="row.overdue" type="danger" size="small" effect="dark">逾期</el-tag>
            </div>
          </template>
        </el-table-column>
        <el-table-column label="评价" width="75">
          <template #default="{ row }">
            <el-tag v-if="row.evaluationScore" type="success" size="small">{{ row.evaluationScore }} 分</el-tag>
            <span v-else class="muted">未评价</span>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="70" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" @click="openDetail(row)">详情</el-button>
          </template>
        </el-table-column>
        <template #empty>
          <el-empty description="没有符合条件的工单" />
        </template>
      </el-table>

      <el-pagination
        v-model:current-page="query.pageNum"
        v-model:page-size="query.pageSize"
        class="pagination"
        background
        layout="total, sizes, prev, pager, next, jumper"
        :page-sizes="[10, 20, 50]"
        :total="total"
        @current-change="load"
        @size-change="search"
      />
    </el-card>
  </div>
</template>

<style scoped>
.page {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.filter-card :deep(.el-form-item) {
  margin-bottom: 8px;
}

.card-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.deadline-cell {
  display: flex;
  align-items: center;
  gap: 6px;
  flex-wrap: wrap;
}

.muted {
  color: var(--sf-text-secondary);
  font-size: 13px;
}

.pagination {
  margin-top: 16px;
  justify-content: flex-end;
}
</style>
