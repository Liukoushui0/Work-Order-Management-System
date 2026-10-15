<script setup>
import { computed, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'

import { acceptTicket, completeTicket, fetchTickets, processTicket } from '@/api/ticket'
import StatusTag from '@/components/StatusTag.vue'
import { formatDateTime } from '@/utils/format'
import { normalizeError } from '@/utils/request'

const router = useRouter()
const loading = ref(false)
const pending = ref([])
const processing = ref([])
const dialog = ref({ visible: false, type: '', ticketId: null, content: '', usedHours: null })

const pendingCount = computed(() => pending.value.length)
const processingCount = computed(() => processing.value.length)

async function load() {
  loading.value = true
  try {
    const [pendingData, processingData] = await Promise.all([
      fetchTickets({ status: 'PENDING_ACCEPT', pageSize: 50 }),
      fetchTickets({ status: 'PROCESSING', pageSize: 50 })
    ])
    pending.value = pendingData.records || []
    processing.value = processingData.records || []
  } catch (error) {
    ElMessage.error(normalizeError(error).message)
  } finally {
    loading.value = false
  }
}

async function doAccept(row) {
  try {
    await ElMessageBox.confirm(`确认接单：${row.ticketNo}？`, '接单确认', { type: 'warning' })
    await acceptTicket(row.id)
    ElMessage.success('接单成功，已进入处理中')
    await load()
  } catch (error) {
    if (error !== 'cancel') ElMessage.error(normalizeError(error).message)
  }
}

function openDialog(type, row) {
  dialog.value = { visible: true, type, ticketId: row.id, content: '', usedHours: null }
}

async function submitDialog() {
  const { type, ticketId, content, usedHours } = dialog.value
  if (!content.trim()) {
    ElMessage.warning('请填写内容')
    return
  }
  try {
    if (type === 'process') {
      await processTicket(ticketId, { content, usedHours })
      ElMessage.success('处理记录已保存')
    } else {
      await completeTicket(ticketId, { result: content, usedHours })
      ElMessage.success('已提交处理结果，等待客户确认')
    }
    dialog.value.visible = false
    await load()
  } catch (error) {
    ElMessage.error(normalizeError(error).message)
  }
}

onMounted(load)
</script>

<template>
  <div v-loading="loading" class="page">
    <el-alert type="info" show-icon :closable="false"
              title="工作台只展示分配给当前账号的工单；接单后请及时填写处理记录并提交处理结果。" />

    <el-card shadow="never">
      <template #header>
        <div class="card-header">
          <span>待接单（{{ pendingCount }}）</span>
          <el-button size="small" @click="load">刷新</el-button>
        </div>
      </template>
      <el-table :data="pending" stripe>
        <el-table-column prop="ticketNo" label="工单编号" width="170" />
        <el-table-column prop="title" label="标题" min-width="180" show-overflow-tooltip />
        <el-table-column prop="customerName" label="客户" min-width="150" show-overflow-tooltip />
        <el-table-column label="优先级" width="90">
          <template #default="{ row }"><StatusTag kind="priority" :value="row.priority" /></template>
        </el-table-column>
        <el-table-column label="截止时间" width="180">
          <template #default="{ row }">
            {{ formatDateTime(row.deadlineTime) }}
            <el-tag v-if="row.overdue" type="danger" size="small" effect="dark">逾期</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="170" fixed="right">
          <template #default="{ row }">
            <el-button type="primary" size="small" @click="doAccept(row)">接单</el-button>
            <el-button link type="primary" @click="router.push(`/tickets/${row.id}`)">详情</el-button>
          </template>
        </el-table-column>
        <template #empty><el-empty description="暂无待接单工单" /></template>
      </el-table>
    </el-card>

    <el-card shadow="never">
      <template #header><span>处理中（{{ processingCount }}）</span></template>
      <el-table :data="processing" stripe>
        <el-table-column prop="ticketNo" label="工单编号" width="170" />
        <el-table-column prop="title" label="标题" min-width="180" show-overflow-tooltip />
        <el-table-column prop="customerName" label="客户" min-width="150" show-overflow-tooltip />
        <el-table-column label="状态" width="100">
          <template #default="{ row }"><StatusTag :value="row.status" /></template>
        </el-table-column>
        <el-table-column label="截止时间" width="180">
          <template #default="{ row }">
            {{ formatDateTime(row.deadlineTime) }}
            <el-tag v-if="row.overdue" type="danger" size="small" effect="dark">逾期</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="230" fixed="right">
          <template #default="{ row }">
            <el-button size="small" @click="openDialog('process', row)">处理记录</el-button>
            <el-button type="success" size="small" @click="openDialog('complete', row)">提交结果</el-button>
            <el-button link type="primary" @click="router.push(`/tickets/${row.id}`)">详情</el-button>
          </template>
        </el-table-column>
        <template #empty><el-empty description="暂无处理中工单" /></template>
      </el-table>
    </el-card>

    <el-dialog v-model="dialog.visible" :title="dialog.type === 'process' ? '填写处理记录' : '提交处理结果'"
               width="520px">
      <el-form label-width="90px">
        <el-form-item :label="dialog.type === 'process' ? '处理说明' : '处理结果'">
          <el-input v-model="dialog.content" type="textarea" :rows="4" maxlength="2000" show-word-limit
                    :placeholder="dialog.type === 'process' ? '记录处理过程' : '说明最终处理结果，供客户确认'" />
        </el-form-item>
        <el-form-item label="本次工时">
          <el-input-number v-model="dialog.usedHours" :min="0" :max="999" :precision="1" />
          <span class="hint">小时</span>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialog.visible = false">取消</el-button>
        <el-button type="primary" @click="submitDialog">提交</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<style scoped>
.page {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.card-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.hint {
  margin-left: 8px;
  font-size: 12px;
  color: var(--sf-text-secondary);
}
</style>
