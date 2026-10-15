<script setup>
import { onMounted, ref } from 'vue'
import { ElMessage } from 'element-plus'

import { fetchHealth } from '@/api/health'
import { normalizeError } from '@/utils/request'

const loading = ref(false)
const errorMessage = ref('')
const health = ref(null)

const statusTagType = value => (value === 'UP' ? 'success' : 'danger')

async function loadHealth() {
  loading.value = true
  errorMessage.value = ''
  try {
    health.value = await fetchHealth()
  } catch (error) {
    health.value = null
    errorMessage.value = normalizeError(error).message
    ElMessage.error(errorMessage.value)
  } finally {
    loading.value = false
  }
}

onMounted(loadHealth)
</script>

<template>
  <div class="dashboard">
    <el-card shadow="never">
      <template #header>
        <div class="card-header">
          <span>系统运行状态</span>
          <el-button type="primary" :loading="loading" @click="loadHealth">刷新</el-button>
        </div>
      </template>

      <el-alert
        v-if="errorMessage"
        type="error"
        :title="errorMessage"
        description="请确认后端服务、MySQL 与 Redis 已启动，然后点击刷新重试。"
        show-icon
        :closable="false"
      />

      <el-skeleton v-else-if="loading && !health" :rows="3" animated />

      <el-descriptions v-else-if="health" :column="2" border>
        <el-descriptions-item label="应用名称">{{ health.application }}</el-descriptions-item>
        <el-descriptions-item label="应用状态">
          <el-tag :type="statusTagType(health.status)">{{ health.status }}</el-tag>
        </el-descriptions-item>
        <el-descriptions-item label="数据库">
          <el-tag :type="statusTagType(health.database)">MySQL {{ health.database }}</el-tag>
        </el-descriptions-item>
        <el-descriptions-item label="缓存">
          <el-tag :type="statusTagType(health.redis)">Redis {{ health.redis }}</el-tag>
        </el-descriptions-item>
        <el-descriptions-item label="服务器时间" :span="2">{{ health.time }}</el-descriptions-item>
      </el-descriptions>

      <el-empty v-else description="暂无数据" />
    </el-card>

    <el-card shadow="never" class="tip-card">
      <template #header><span>后续阶段说明</span></template>
      <p>
        当前为 Stage 1（工程骨架）阶段：已打通前端 → 后端 → MySQL / Redis
        的完整链路。工单统计图表将在实现统计接口后接入本页，图表数据全部来自后端真实统计结果。
      </p>
    </el-card>
  </div>
</template>

<style scoped>
.dashboard {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.card-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.tip-card p {
  margin: 0;
  color: var(--sf-text-secondary);
  line-height: 1.7;
}
</style>
