<script setup>
import { useRouter } from 'vue-router'

const router = useRouter()

function backToDashboard() {
  router.push('/dashboard')
}
</script>

<template>
  <div class="not-found">
    <el-result icon="warning" title="404" sub-title="抱歉，你访问的页面不存在或已被移除">
      <template #extra>
        <el-button type="primary" @click="backToDashboard">返回仪表盘</el-button>
      </template>
    </el-result>
  </div>
</template>

<style scoped>
.not-found {
  min-height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  background: var(--sf-bg-color);
}
</style>
