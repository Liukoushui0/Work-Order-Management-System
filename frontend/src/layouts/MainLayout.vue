<script setup>
import { computed } from 'vue'
import { useRoute } from 'vue-router'

const route = useRoute()

const breadcrumbs = computed(() => {
  const items = route.meta.breadcrumb
  return items && items.length > 0 ? items : ['首页']
})

const menus = [
  { index: '/dashboard', title: '售后运营仪表盘', icon: 'DataLine' }
]
</script>

<template>
  <el-container class="app-shell">
    <el-aside width="220px" class="app-aside">
      <div class="app-brand">ServiceFlow</div>
      <el-menu :default-active="route.path" router class="app-menu">
        <el-menu-item v-for="menu in menus" :key="menu.index" :index="menu.index">
          <el-icon><component :is="menu.icon" /></el-icon>
          <span>{{ menu.title }}</span>
        </el-menu-item>
      </el-menu>
    </el-aside>

    <el-container>
      <el-header class="app-header">
        <div class="app-header__title">企业售后工单管理系统</div>
        <div class="app-header__user">本地开发环境</div>
      </el-header>
      <el-main class="app-main">
        <el-breadcrumb separator="/" class="app-breadcrumb">
          <el-breadcrumb-item v-for="(item, index) in breadcrumbs" :key="index">
            {{ item }}
          </el-breadcrumb-item>
        </el-breadcrumb>
        <router-view />
      </el-main>
    </el-container>
  </el-container>
</template>

<style scoped>
.app-shell {
  min-height: 100vh;
}

.app-aside {
  background: #ffffff;
  border-right: 1px solid var(--sf-border-color);
}

.app-brand {
  height: 60px;
  display: flex;
  align-items: center;
  padding: 0 20px;
  font-size: 18px;
  font-weight: 600;
  color: var(--sf-primary);
  letter-spacing: 0.5px;
}

.app-menu {
  border-right: none;
}

.app-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  background: #ffffff;
  border-bottom: 1px solid var(--sf-border-color);
}

.app-header__title {
  font-size: 16px;
  font-weight: 600;
  color: var(--sf-text-primary);
}

.app-header__user {
  font-size: 13px;
  color: var(--sf-text-secondary);
}

.app-main {
  background: var(--sf-bg-color);
}

.app-breadcrumb {
  margin-bottom: 16px;
}
</style>
