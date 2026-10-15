<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'

import { createEvaluation, fetchTickets } from '@/api/ticket'
import { fetchSatisfaction } from '@/api/dashboard'
import StatusTag from '@/components/StatusTag.vue'
import { useAuthStore } from '@/stores/auth'
import { satisfactionLabel } from '@/utils/format'
import { normalizeError } from '@/utils/request'

const router = useRouter()
const authStore = useAuthStore()
const loading = ref(false)
const records = ref([])
const total = ref(0)
const satisfaction = ref(null)
const submitting = ref(false)
const evaluatedFilter = ref('ALL')

const query = reactive({ status: 'COMPLETED', pageNum: 1, pageSize: 10 })
const dialog = reactive({ visible: false, ticketId: null, ticketNo: '', score: 5, content: '', tags: '', anonymous: false })

const filteredRecords = computed(() => {
  if (evaluatedFilter.value === 'EVALUATED') return records.value.filter(row => row.evaluationScore)
  if (evaluatedFilter.value === 'PENDING') return records.value.filter(row => !row.evaluationScore)
  return records.value
})

async function load() {
  loading.value = true
  try {
    const data = await fetchTickets({ ...query })
    records.value = data.records || []
    total.value = data.total || 0
    if (!authStore.isCustomer) {
      satisfaction.value = await fetchSatisfaction().catch(() => null)
    }
  } catch (error) {
    ElMessage.error(normalizeError(error).message)
  } finally {
    loading.value = false
  }
}

function openEvaluate(row) {
  dialog.visible = true
  dialog.ticketId = row.id
  dialog.ticketNo = row.ticketNo
  dialog.score = 5
  dialog.content = ''
  dialog.tags = ''
  dialog.anonymous = false
}

async function submit() {
  submitting.value = true
  try {
    await createEvaluation(dialog.ticketId, {
      score: dialog.score,
      content: dialog.content,
      tags: dialog.tags,
      anonymous: dialog.anonymous
    })
    ElMessage.success('评价已提交，感谢反馈')
    dialog.visible = false
    await load()
  } catch (error) {
    ElMessage.error(normalizeError(error).message)
  } finally {
    submitting.value = false
  }
}

onMounted(load)
</script>

<template>
  <div class="page">
    <el-alert v-if="authStore.isCustomer" type="info" show-icon :closable="false"
              title="工单完成并确认后可以评价；每个工单只能评价一次，评价提交后不可修改。" />

    <div v-if="!authStore.isCustomer && satisfaction" class="stat-grid">
      <el-card shadow="never" class="stat-card">
        <div class="stat-value">{{ satisfaction.total }}</div>
        <div class="stat-label">评价总数</div>
      </el-card>
      <el-card shadow="never" class="stat-card">
        <div class="stat-value">{{ satisfaction.avgScore === null ? '-' : satisfaction.avgScore }}</div>
        <div class="stat-label">平均评分</div>
      </el-card>
      <el-card v-for="item in satisfaction.distribution || []" :key="item.key" shadow="never" class="stat-card">
        <div class="stat-value">{{ item.value }}</div>
        <div class="stat-label">{{ item.name }}（{{ item.percent }}%）</div>
      </el-card>
    </div>

    <el-card shadow="never">
      <template #header>
        <div class="card-header">
          <span>已完成工单（共 {{ total }} 条）</span>
          <div class="card-header__actions">
            <el-radio-group v-model="evaluatedFilter" size="small">
              <el-radio-button value="ALL">全部</el-radio-button>
              <el-radio-button value="EVALUATED">已评价</el-radio-button>
              <el-radio-button value="PENDING">待评价</el-radio-button>
            </el-radio-group>
            <el-button size="small" @click="load">刷新</el-button>
          </div>
        </div>
      </template>

      <el-table v-loading="loading" :data="filteredRecords" stripe>
        <el-table-column prop="ticketNo" label="工单编号" width="170" />
        <el-table-column prop="title" label="标题" min-width="180" show-overflow-tooltip />
        <el-table-column prop="customerName" label="客户" min-width="150" show-overflow-tooltip />
        <el-table-column prop="assigneeName" label="负责人" width="120" />
        <el-table-column label="状态" width="100">
          <template #default="{ row }"><StatusTag :value="row.status" /></template>
        </el-table-column>
        <el-table-column label="评价" width="180">
          <template #default="{ row }">
            <template v-if="row.evaluationScore">
              <el-rate :model-value="row.evaluationScore" disabled size="small" />
              <span class="muted">{{ satisfactionLabel(row.evaluationSatisfaction) }}</span>
            </template>
            <span v-else class="muted">未评价</span>
          </template>
        </el-table-column>
        <el-table-column label="处理时长" width="110">
          <template #default="{ row }">
            {{ row.handleHours === null || row.handleHours === undefined ? '-' : `${row.handleHours} h` }}
          </template>
        </el-table-column>
        <el-table-column label="操作" width="150" fixed="right">
          <template #default="{ row }">
            <el-button v-if="authStore.isCustomer && !row.evaluationScore" type="warning" size="small"
                       @click="openEvaluate(row)">去评价</el-button>
            <el-button link type="primary" @click="router.push(`/tickets/${row.id}`)">详情</el-button>
          </template>
        </el-table-column>
        <template #empty><el-empty description="暂无已完成工单" /></template>
      </el-table>

      <el-pagination v-model:current-page="query.pageNum" v-model:page-size="query.pageSize" class="pagination"
                     background layout="total, prev, pager, next" :total="total"
                     @current-change="load" />
    </el-card>

    <el-dialog v-model="dialog.visible" :title="`评价工单 ${dialog.ticketNo}`" width="520px">
      <el-form label-width="90px">
        <el-form-item label="综合评分" required>
          <el-rate v-model="dialog.score" />
        </el-form-item>
        <el-form-item label="评价内容">
          <el-input v-model="dialog.content" type="textarea" :rows="3" maxlength="1000" show-word-limit
                    placeholder="请描述本次服务体验" />
        </el-form-item>
        <el-form-item label="评价标签">
          <el-input v-model="dialog.tags" placeholder="多个标签用逗号分隔，如：响应及时,一次修复" />
        </el-form-item>
        <el-form-item label="匿名评价">
          <el-switch v-model="dialog.anonymous" />
          <span class="muted hint">开启后客服与工程师看不到评价内容</span>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialog.visible = false">取消</el-button>
        <el-button type="primary" :loading="submitting" @click="submit">提交评价</el-button>
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

.stat-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(160px, 1fr));
  gap: 16px;
}

.stat-card {
  text-align: center;
}

.stat-value {
  font-size: 22px;
  font-weight: 600;
  color: var(--sf-primary);
}

.stat-label {
  font-size: 13px;
  color: var(--sf-text-secondary);
}

.card-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.card-header__actions {
  display: flex;
  align-items: center;
  gap: 12px;
}

.muted {
  color: var(--sf-text-secondary);
  font-size: 13px;
}

.hint {
  margin-left: 8px;
}

.pagination {
  margin-top: 16px;
  justify-content: flex-end;
}
</style>
