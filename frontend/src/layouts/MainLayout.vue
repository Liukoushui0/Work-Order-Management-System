<script setup>
import { computed, onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'

import { buildMenus } from '@/router'
import { useAuthStore } from '@/stores/auth'

const route = useRoute()
const router = useRouter()
const authStore = useAuthStore()
const collapsed = ref(false)

const roleNameMap = {
  ROLE_ADMIN: '系统管理员',
  ROLE_SERVICE: '售后客服',
  ROLE_ENGINEER: '维修工程师',
  ROLE_CUSTOMER: '客户'
}

const menus = computed(() => buildMenus(authStore))
const breadcrumbs = computed(() => {
  const items = route.meta?.breadcrumb
  return items && items.length > 0 ? items : ['首页']
})
const roleNames = computed(() => authStore.roles.map(role => roleNameMap[role] || role).join(' / '))
const activeMenu = computed(() => {
  if (route.path.startsWith('/tickets/') && route.path !== '/tickets') return '/tickets'
  return route.path
})

async function handleCommand(command) {
  if (command === 'profile') {
    router.push('/profile')
    return
  }
  if (command === 'logout') {
    await ElMessageBox.confirm('确认退出登录？', '提示', { type: 'warning' })
    await authStore.logout()
    ElMessage.success('已退出登录')
    router.push('/login')
  }
}

onMounted(() => {
  if (!authStore.user && authStore.isLoggedIn) {
    authStore.loadCurrentUser().catch(() => authStore.clear())
  }
})
</script>

<template>
  <el-container class="app-shell">
    <el-aside :width="collapsed ? '64px' : '220px'" class="app-aside">
      <div class="app-brand">
        <span class="app-brand__mark">SF</span>
        <span v-show="!collapsed" class="app-brand__text">ServiceFlow</span>
      </div>
      <el-menu :default-active="activeMenu" :collapse="collapsed" router class="app-menu">
        <el-menu-item v-for="menu in menus" :key="menu.index" :index="menu.index">
          <el-icon><component :is="menu.icon" /></el-icon>
          <template #title>{{ menu.title }}</template>
        </el-menu-item>
      </el-menu>
    </el-aside>

    <el-container>
      <el-header class="app-header">
        <div class="app-header__left">
          <el-icon class="app-header__toggle" @click="collapsed = !collapsed">
            <component :is="collapsed ? 'Expand' : 'Fold'" />
          </el-icon>
          <span class="app-header__title">企业售后工单管理系统</span>
        </div>
        <div class="app-header__right">
          <span class="app-header__role">{{ roleNames }}</span>
          <el-dropdown @command="handleCommand">
            <span class="app-header__user">
              <el-icon><UserFilled /></el-icon>
              {{ authStore.displayName }}
              <el-icon><ArrowDown /></el-icon>
            </span>
            <template #dropdown>
              <el-dropdown-menu>
                <el-dropdown-item command="profile">个人中心</el-dropdown-item>
                <el-dropdown-item command="logout" divided>退出登录</el-dropdown-item>
              </el-dropdown-menu>
            </template>
          </el-dropdown>
        </div>
      </el-header>

      <el-main class="app-main">
        <el-breadcrumb separator="/" class="app-breadcrumb">
          <el-breadcrumb-item v-for="(item, index) in breadcrumbs" :key="index">{{ item }}</el-breadcrumb-item>
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
  transition: width 0.2s;
}

.app-brand {
  height: 60px;
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 0 16px;
  border-bottom: 1px solid var(--sf-border-color);
}

.app-brand__mark {
  width: 32px;
  height: 32px;
  border-radius: 8px;
  background: var(--sf-primary);
  color: #fff;
  font-weight: 700;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 14px;
}

.app-brand__text {
  font-size: 17px;
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

.app-header__left {
  display: flex;
  align-items: center;
  gap: 12px;
}

.app-header__toggle {
  cursor: pointer;
  font-size: 18px;
  color: var(--sf-text-secondary);
}

.app-header__title {
  font-size: 16px;
  font-weight: 600;
  color: var(--sf-text-primary);
}

.app-header__right {
  display: flex;
  align-items: center;
  gap: 16px;
}

.app-header__role {
  font-size: 13px;
  color: var(--sf-text-secondary);
}

.app-header__user {
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 14px;
  color: var(--sf-text-primary);
  cursor: pointer;
}

.app-main {
  background: var(--sf-bg-color);
}

.app-breadcrumb {
  margin-bottom: 16px;
}
</style>
