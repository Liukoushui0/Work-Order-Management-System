<script setup>
import { onMounted, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'

import { fetchConfigs, updateConfig } from '@/api/system'
import { formatDateTime } from '@/utils/format'
import { normalizeError } from '@/utils/request'

const loading = ref(false)
const configs = ref([])
const dialog = reactive({ visible: false, configKey: '', configName: '', configValue: '', valueType: 'STRING' })
const submitting = ref(false)

async function load() {
  loading.value = true
  try {
    configs.value = (await fetchConfigs()) || []
  } catch (error) {
    ElMessage.error(normalizeError(error).message)
  } finally {
    loading.value = false
  }
}

function openEdit(row) {
  dialog.visible = true
  dialog.configKey = row.configKey
  dialog.configName = row.configName
  dialog.configValue = row.configValue
  dialog.valueType = row.valueType
}

async function submit() {
  if (dialog.configValue === '' || dialog.configValue === null) {
    ElMessage.warning('配置值不能为空')
    return
  }
  if (dialog.valueType === 'NUMBER' && Number.isNaN(Number(dialog.configValue))) {
    ElMessage.warning('该配置项需要是数字')
    return
  }
  if (dialog.valueType === 'BOOLEAN' && !['true', 'false'].includes(String(dialog.configValue))) {
    ElMessage.warning('该配置项需要是 true 或 false')
    return
  }
  submitting.value = true
  try {
    await updateConfig(dialog.configKey, String(dialog.configValue))
    ElMessage.success('配置已更新')
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
    <el-alert type="warning" show-icon :closable="false"
              title="系统配置会影响业务流程：确认时限、SLA、附件限制与登录锁定策略均在此维护，修改后立即生效。" />

    <el-card shadow="never">
      <template #header>
        <div class="card-header">
          <span>系统配置（共 {{ configs.length }} 项）</span>
          <el-button size="small" @click="load">刷新</el-button>
        </div>
      </template>
      <el-table v-loading="loading" :data="configs" stripe>
        <el-table-column prop="configName" label="配置名称" width="190" />
        <el-table-column prop="configKey" label="配置键" width="260" />
        <el-table-column prop="configValue" label="当前值" width="200" />
        <el-table-column prop="configGroup" label="分组" width="110" />
        <el-table-column prop="valueType" label="类型" width="100" />
        <el-table-column prop="remark" label="说明" min-width="240" show-overflow-tooltip />
        <el-table-column label="更新时间" width="170">
          <template #default="{ row }">{{ formatDateTime(row.updateTime) }}</template>
        </el-table-column>
        <el-table-column label="操作" width="90" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" @click="openEdit(row)">修改</el-button>
          </template>
        </el-table-column>
        <template #empty><el-empty description="暂无配置项" /></template>
      </el-table>
    </el-card>

    <el-dialog v-model="dialog.visible" :title="`修改配置 - ${dialog.configName}`" width="520px">
      <el-form label-width="90px">
        <el-form-item label="配置键"><el-input :model-value="dialog.configKey" disabled /></el-form-item>
        <el-form-item label="配置值" required>
          <el-switch v-if="dialog.valueType === 'BOOLEAN'" v-model="dialog.configValue" active-value="true"
                     inactive-value="false" />
          <el-input v-else v-model="dialog.configValue" :type="dialog.valueType === 'NUMBER' ? 'number' : 'text'" />
        </el-form-item>
        <el-form-item label="值类型"><el-input :model-value="dialog.valueType" disabled /></el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialog.visible = false">取消</el-button>
        <el-button type="primary" :loading="submitting" @click="submit">保存</el-button>
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
</style>
