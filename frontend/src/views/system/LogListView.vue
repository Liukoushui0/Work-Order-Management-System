<script setup>
import { onMounted, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'

import { fetchOperationLogs } from '@/api/system'
import { formatDateTime } from '@/utils/format'
import { normalizeError } from '@/utils/request'

const loading = ref(false)
const records = ref([])
const total = ref(0)
const dateRange = ref([])
const detail = reactive({ visible: false, log: null })

const query = reactive({ module: '', username: '', success: null, pageNum: 1, pageSize: 10 })

const moduleOptions = [
  { value: 'auth', label: '认证' },
  { value: 'user', label: '用户管理' },
  { value: 'customer', label: '客户管理' },
  { value: 'device', label: '设备档案' },
  { value: 'ticket', label: '工单' },
  { value: 'evaluation', label: '评价' },
  { value: 'attachment', label: '附件' },
  { value: 'fault-category', label: '故障分类' },
  { value: 'config', label: '系统配置' }
]

async function load() {
  loading.value = true
  try {
    const params = { ...query }
    Object.keys(params).forEach(key => {
      if (params[key] === null || params[key] === '') delete params[key]
    })
    if (dateRange.value && dateRange.value.length === 2) {
      params.startDate = dateRange.value[0]
      params.endDate = dateRange.value[1]
    }
    const data = await fetchOperationLogs(params)
    records.value = data.records || []
    total.value = data.total || 0
  } catch (error) {
    ElMessage.error(normalizeError(error).message)
  } finally {
    loading.value = false
  }
}

function openDetail(row) {
  detail.log = row
  detail.visible = true
}

onMounted(load)
</script>

<template>
  <div class="page">
    <el-card shadow="never">
      <el-form :inline="true" @submit.prevent>
        <el-form-item label="模块">
          <el-select v-model="query.module" placeholder="全部模块" clearable style="width: 150px">
            <el-option v-for="item in moduleOptions" :key="item.value" :label="item.label" :value="item.value" />
          </el-select>
        </el-form-item>
        <el-form-item label="操作人">
          <el-input v-model="query.username" placeholder="账号" clearable style="width: 150px"
                    @keyup.enter="load" />
        </el-form-item>
        <el-form-item label="结果">
          <el-select v-model="query.success" placeholder="全部" clearable style="width: 110px">
            <el-option label="成功" :value="1" />
            <el-option label="失败" :value="0" />
          </el-select>
        </el-form-item>
        <el-form-item label="日期">
          <el-date-picker v-model="dateRange" type="daterange" value-format="YYYY-MM-DD" start-placeholder="开始"
                          end-placeholder="结束" style="width: 240px" />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="load">查询</el-button>
          <el-button @click="() => { query.module = ''; query.username = ''; query.success = null; dateRange = []; load() }">
            重置
          </el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <el-card shadow="never">
      <template #header><span>操作日志（共 {{ total }} 条）</span></template>
      <el-table v-loading="loading" :data="records" stripe>
        <el-table-column label="时间" width="170">
          <template #default="{ row }">{{ formatDateTime(row.createTime) }}</template>
        </el-table-column>
        <el-table-column prop="username" label="操作人" width="120">
          <template #default="{ row }">{{ row.username || '-' }}</template>
        </el-table-column>
        <el-table-column prop="module" label="模块" width="120" />
        <el-table-column prop="operation" label="操作" width="170" />
        <el-table-column prop="requestUri" label="请求路径" min-width="220" show-overflow-tooltip />
        <el-table-column label="结果" width="100">
          <template #default="{ row }">
            <el-tag :type="row.success === 1 ? 'success' : 'danger'" size="small">
              {{ row.success === 1 ? '成功' : '失败' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="耗时" width="100">
          <template #default="{ row }">{{ row.durationMs }} ms</template>
        </el-table-column>
        <el-table-column prop="ip" label="IP" width="140" />
        <el-table-column label="操作" width="90" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" @click="openDetail(row)">详情</el-button>
          </template>
        </el-table-column>
        <template #empty><el-empty description="没有符合条件的日志" /></template>
      </el-table>

      <el-pagination v-model:current-page="query.pageNum" v-model:page-size="query.pageSize" class="pagination"
                     background layout="total, sizes, prev, pager, next" :page-sizes="[10, 20, 50]" :total="total"
                     @current-change="load" @size-change="load" />
    </el-card>

    <el-dialog v-model="detail.visible" title="日志详情" width="640px">
      <el-descriptions v-if="detail.log" :column="2" border>
        <el-descriptions-item label="时间">{{ formatDateTime(detail.log.createTime) }}</el-descriptions-item>
        <el-descriptions-item label="traceId">{{ detail.log.traceId || '-' }}</el-descriptions-item>
        <el-descriptions-item label="操作人">{{ detail.log.username || '-' }}</el-descriptions-item>
        <el-descriptions-item label="模块">{{ detail.log.module }}</el-descriptions-item>
        <el-descriptions-item label="操作">{{ detail.log.operation }}</el-descriptions-item>
        <el-descriptions-item label="方法">{{ detail.log.requestMethod }}</el-descriptions-item>
        <el-descriptions-item label="响应码">{{ detail.log.responseCode ?? '-' }}</el-descriptions-item>
        <el-descriptions-item label="耗时">{{ detail.log.durationMs }} ms</el-descriptions-item>
        <el-descriptions-item label="请求参数" :span="2">
          <pre class="params">{{ detail.log.requestParams || '-' }}</pre>
        </el-descriptions-item>
        <el-descriptions-item v-if="detail.log.errorMsg" label="错误信息" :span="2">
          {{ detail.log.errorMsg }}
        </el-descriptions-item>
      </el-descriptions>
    </el-dialog>
  </div>
</template>

<style scoped>
.page {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.pagination {
  margin-top: 16px;
  justify-content: flex-end;
}

.params {
  margin: 0;
  white-space: pre-wrap;
  word-break: break-all;
  font-family: Consolas, Monaco, monospace;
  font-size: 12px;
}
</style>
