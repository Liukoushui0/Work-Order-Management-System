<script setup>
import { computed, onMounted, ref } from 'vue'
import { ElMessage } from 'element-plus'

import {
  fetchEngineerWorkload,
  fetchFaultDistribution,
  fetchOverview,
  fetchPriorityDistribution,
  fetchSatisfaction,
  fetchTrend
} from '@/api/dashboard'
import BaseChart from '@/components/BaseChart.vue'
import { useAuthStore } from '@/stores/auth'
import { normalizeError } from '@/utils/request'

const authStore = useAuthStore()
const loading = ref(false)
const errorMessage = ref('')
const overview = ref(null)
const trend = ref([])
const faultDistribution = ref([])
const priorityDistribution = ref([])
const workload = ref([])
const satisfaction = ref(null)
const granularity = ref('DAY')

const statCards = computed(() => {
  const data = overview.value
  if (!data) return []
  return [
    { label: '工单总数', value: data.total, icon: 'Tickets', color: '#2b6cff' },
    { label: '进行中', value: data.openCount, icon: 'Loading', color: '#e6a23c' },
    { label: '逾期', value: data.overdueCount, icon: 'AlarmClock', color: '#f56c6c' },
    { label: '已完成', value: data.completedCount, icon: 'CircleCheck', color: '#67c23a' },
    {
      label: '平均处理时长',
      value: data.avgHandleHours === null || data.avgHandleHours === undefined ? '-' : `${data.avgHandleHours} h`,
      icon: 'Timer',
      color: '#909399'
    },
    {
      label: '平均满意度',
      value: data.avgSatisfaction === null || data.avgSatisfaction === undefined
        ? '暂无评价'
        : `${data.avgSatisfaction} 分`,
      icon: 'Star',
      color: '#f7ba2a'
    }
  ]
})

const trendOption = computed(() => ({
  tooltip: { trigger: 'axis' },
  legend: { data: ['新建工单', '完成工单'], right: 10, top: 0 },
  grid: { left: 40, right: 20, top: 40, bottom: 30 },
  xAxis: { type: 'category', boundaryGap: false, data: trend.value.map(item => item.period) },
  yAxis: { type: 'value', minInterval: 1 },
  series: [
    {
      name: '新建工单',
      type: 'line',
      smooth: true,
      areaStyle: { opacity: 0.12 },
      itemStyle: { color: '#2b6cff' },
      data: trend.value.map(item => item.created)
    },
    {
      name: '完成工单',
      type: 'line',
      smooth: true,
      areaStyle: { opacity: 0.12 },
      itemStyle: { color: '#67c23a' },
      data: trend.value.map(item => item.completed)
    }
  ]
}))

function pieOption(items) {
  return {
    tooltip: { trigger: 'item', formatter: '{b}: {c} ({d}%)' },
    legend: { bottom: 0, type: 'scroll' },
    series: [
      {
        type: 'pie',
        radius: ['42%', '68%'],
        center: ['50%', '45%'],
        avoidLabelOverlap: true,
        label: { formatter: '{b}\n{c}' },
        data: items.map(item => ({ name: item.name, value: item.value }))
      }
    ]
  }
}

const faultOption = computed(() => pieOption(faultDistribution.value))
const priorityOption = computed(() => pieOption(priorityDistribution.value))

const workloadOption = computed(() => ({
  tooltip: { trigger: 'axis', axisPointer: { type: 'shadow' } },
  legend: { data: ['已完成', '进行中'], right: 10, top: 0 },
  grid: { left: 70, right: 20, top: 40, bottom: 30 },
  xAxis: { type: 'value', minInterval: 1 },
  yAxis: { type: 'category', data: workload.value.map(item => item.engineerName) },
  series: [
    {
      name: '已完成',
      type: 'bar',
      stack: 'total',
      itemStyle: { color: '#67c23a' },
      data: workload.value.map(item => item.completed)
    },
    {
      name: '进行中',
      type: 'bar',
      stack: 'total',
      itemStyle: { color: '#e6a23c' },
      data: workload.value.map(item => item.open)
    }
  ]
}))

const satisfactionOption = computed(() => ({
  tooltip: { trigger: 'axis' },
  grid: { left: 40, right: 20, top: 20, bottom: 30 },
  xAxis: { type: 'category', data: (satisfaction.value?.distribution || []).map(item => item.name) },
  yAxis: { type: 'value', minInterval: 1 },
  series: [
    {
      type: 'bar',
      barWidth: '46%',
      itemStyle: { color: '#2b6cff', borderRadius: [4, 4, 0, 0] },
      data: (satisfaction.value?.distribution || []).map(item => item.value)
    }
  ]
}))

async function loadAll() {
  loading.value = true
  errorMessage.value = ''
  try {
    const [overviewData, trendData, faultData, priorityData, workloadData, satisfactionData] = await Promise.all([
      fetchOverview(),
      fetchTrend({ granularity: granularity.value }),
      fetchFaultDistribution(),
      fetchPriorityDistribution(),
      fetchEngineerWorkload(),
      fetchSatisfaction()
    ])
    overview.value = overviewData
    trend.value = trendData || []
    faultDistribution.value = faultData || []
    priorityDistribution.value = priorityData || []
    workload.value = workloadData || []
    satisfaction.value = satisfactionData
  } catch (error) {
    errorMessage.value = normalizeError(error).message
    ElMessage.error(errorMessage.value)
  } finally {
    loading.value = false
  }
}

async function reloadTrend() {
  try {
    trend.value = (await fetchTrend({ granularity: granularity.value })) || []
  } catch (error) {
    ElMessage.error(normalizeError(error).message)
  }
}

onMounted(loadAll)
</script>

<template>
  <div v-loading="loading" class="dashboard">
    <el-alert
      v-if="errorMessage"
      type="error"
      :title="errorMessage"
      description="统计数据加载失败，请确认后端与数据库服务正常后重试。"
      show-icon
      :closable="false"
    />

    <div class="stat-grid">
      <el-card v-for="card in statCards" :key="card.label" shadow="never" class="stat-card">
        <span class="stat-card__accent" :style="{ background: card.color }" />
        <div class="stat-card__body">
          <el-icon :style="{ background: `${card.color}1a`, color: card.color }" class="stat-card__icon">
            <component :is="card.icon" />
          </el-icon>
          <div>
            <div class="stat-card__value">{{ card.value }}</div>
            <div class="stat-card__label">{{ card.label }}</div>
          </div>
        </div>
      </el-card>
    </div>

    <el-card shadow="never">
      <template #header>
        <div class="card-header">
          <span>工单趋势{{ overview?.scope === 'SELF' ? '（仅本人）' : '' }}</span>
          <div class="card-header__actions">
            <el-radio-group v-model="granularity" size="small" @change="reloadTrend">
              <el-radio-button value="DAY">按日</el-radio-button>
              <el-radio-button value="WEEK">按周</el-radio-button>
              <el-radio-button value="MONTH">按月</el-radio-button>
            </el-radio-group>
            <el-button size="small" @click="loadAll">刷新</el-button>
          </div>
        </div>
      </template>
      <BaseChart v-if="trend.length" :option="trendOption" :loading="loading" />
      <el-empty v-else description="所选区间暂无工单数据" />
    </el-card>

    <el-row :gutter="16">
      <el-col :xs="24" :md="12">
        <el-card shadow="never">
          <template #header><span>故障类型分布</span></template>
          <BaseChart v-if="faultDistribution.length" :option="faultOption" height="300px" />
          <el-empty v-else description="暂无故障分类数据" />
        </el-card>
      </el-col>
      <el-col :xs="24" :md="12">
        <el-card shadow="never">
          <template #header><span>优先级分布</span></template>
          <BaseChart v-if="priorityDistribution.length" :option="priorityOption" height="300px" />
          <el-empty v-else description="暂无工单数据" />
        </el-card>
      </el-col>
    </el-row>

    <el-row :gutter="16">
      <el-col :xs="24" :md="authStore.isEngineer ? 24 : 12">
        <el-card shadow="never">
          <template #header><span>维修人员工作量</span></template>
          <BaseChart v-if="workload.length" :option="workloadOption" height="300px" />
          <el-empty v-else description="暂无已分派工单" />
        </el-card>
      </el-col>
      <el-col v-if="!authStore.isEngineer" :xs="24" :md="12">
        <el-card shadow="never">
          <template #header>
            <span>客户满意度{{ satisfaction ? `（共 ${satisfaction.total} 条评价）` : '' }}</span>
          </template>
          <BaseChart v-if="satisfaction && satisfaction.total > 0" :option="satisfactionOption" height="300px" />
          <el-empty v-else description="暂无客户评价" />
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<style scoped>
.dashboard {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.stat-grid {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 16px;
}

@media (max-width: 1200px) {
  .stat-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}

@media (max-width: 720px) {
  .stat-grid {
    grid-template-columns: minmax(0, 1fr);
  }
}

.stat-card__body {
  display: flex;
  align-items: center;
  gap: 14px;
  padding-left: 6px;
}

.stat-card__accent {
  position: absolute;
  left: 0;
  top: 12px;
  bottom: 12px;
  width: 4px;
  border-radius: 0 4px 4px 0;
}

.stat-card__icon {
  width: 44px;
  height: 44px;
  border-radius: 12px;
  font-size: 20px;
  display: flex;
  align-items: center;
  justify-content: center;
}

.stat-card__value {
  font-size: 22px;
  font-weight: 600;
  color: var(--sf-text-primary);
  line-height: 1.2;
}

.stat-card__label {
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
</style>
