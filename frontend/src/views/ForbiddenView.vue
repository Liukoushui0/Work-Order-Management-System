<script setup>
import { useRouter } from 'vue-router'

const router = useRouter()
</script>

<template>
  <div class="forbidden">
    <el-result icon="error" title="403" sub-title="当前账号没有访问该页面的权限，如需开通请联系系统管理员">
      <template #extra>
        <el-button type="primary" @click="router.push('/dashboard')">返回仪表盘</el-button>
        <el-button @click="router.push('/tickets')">前往工单列表</el-button>
      </template>
    </el-result>
  </div>
</template>

<style scoped>
.forbidden {
  min-height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  background: var(--sf-bg-color);
}
</style>
