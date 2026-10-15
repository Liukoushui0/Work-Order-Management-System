<script setup>
import { onMounted, ref } from 'vue'
import { ElMessage } from 'element-plus'

import { fetchPermissions, fetchRoles } from '@/api/system'
import { normalizeError } from '@/utils/request'

const loading = ref(false)
const roles = ref([])
const permissions = ref([])

async function load() {
  loading.value = true
  try {
    const [roleData, permissionData] = await Promise.all([fetchRoles(), fetchPermissions()])
    roles.value = roleData || []
    permissions.value = permissionData || []
  } catch (error) {
    ElMessage.error(normalizeError(error).message)
  } finally {
    loading.value = false
  }
}

onMounted(load)
</script>

<template>
  <div v-loading="loading" class="page">
    <el-alert type="info" show-icon :closable="false"
              title="四种角色为系统内置，权限在后端按接口强制校验；如需调整授权，请修改 db/data.sql 中的角色权限关系后重新执行脚本。" />

    <el-card shadow="never">
      <template #header><span>角色列表</span></template>
      <el-table :data="roles" stripe>
        <el-table-column prop="roleCode" label="角色编码" width="180" />
        <el-table-column prop="roleName" label="角色名称" width="150" />
        <el-table-column prop="description" label="职责说明" min-width="320" show-overflow-tooltip />
        <el-table-column prop="sort" label="排序" width="80" />
      </el-table>
    </el-card>

    <el-card shadow="never">
      <template #header><span>权限清单（共 {{ permissions.length }} 项）</span></template>
      <el-table :data="permissions" stripe max-height="520">
        <el-table-column prop="permCode" label="权限编码" width="200" />
        <el-table-column prop="permName" label="权限名称" width="180" />
        <el-table-column prop="module" label="所属模块" width="140" />
        <el-table-column prop="permType" label="类型" width="90" />
        <el-table-column prop="description" label="说明" min-width="220" show-overflow-tooltip />
      </el-table>
    </el-card>
  </div>
</template>

<style scoped>
.page {
  display: flex;
  flex-direction: column;
  gap: 16px;
}
</style>
