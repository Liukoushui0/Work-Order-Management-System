import { createRouter, createWebHistory } from 'vue-router'

import MainLayout from '@/layouts/MainLayout.vue'
import { useAuthStore } from '@/stores/auth'

const ADMIN = 'ROLE_ADMIN'
const SERVICE = 'ROLE_SERVICE'
const ENGINEER = 'ROLE_ENGINEER'
const CUSTOMER = 'ROLE_CUSTOMER'

/**
 * 路由表。meta.roles 为空表示所有登录用户可见；
 * meta.menu 用于侧边栏展示，meta.breadcrumb 用于面包屑。
 */
export const routes = [
  {
    path: '/login',
    name: 'login',
    component: () => import('@/views/LoginView.vue'),
    meta: { title: '登录', public: true }
  },
  {
    path: '/',
    component: MainLayout,
    redirect: '/dashboard',
    children: [
      {
        path: 'dashboard',
        name: 'dashboard',
        component: () => import('@/views/DashboardView.vue'),
        meta: {
          title: '售后运营仪表盘',
          roles: [ADMIN, SERVICE, ENGINEER],
          menu: { icon: 'DataLine' },
          breadcrumb: ['首页', '售后运营仪表盘']
        }
      },
      {
        path: 'tickets',
        name: 'ticket-list',
        component: () => import('@/views/ticket/TicketListView.vue'),
        meta: { title: '工单列表', menu: { icon: 'Tickets' }, breadcrumb: ['首页', '工单管理', '工单列表'] }
      },
      {
        path: 'tickets/create',
        name: 'ticket-create',
        component: () => import('@/views/ticket/TicketCreateView.vue'),
        meta: { title: '创建工单', roles: [ADMIN, SERVICE], breadcrumb: ['首页', '工单管理', '创建工单'] }
      },
      {
        path: 'tickets/:id',
        name: 'ticket-detail',
        component: () => import('@/views/ticket/TicketDetailView.vue'),
        meta: { title: '工单详情', breadcrumb: ['首页', '工单管理', '工单详情'] }
      },
      {
        path: 'workspace',
        name: 'ticket-workspace',
        component: () => import('@/views/ticket/TicketWorkspaceView.vue'),
        meta: {
          title: '工单处理工作台',
          roles: [ENGINEER, ADMIN],
          menu: { icon: 'Tools' },
          breadcrumb: ['首页', '工单处理工作台']
        }
      },
      {
        path: 'customers',
        name: 'customer-list',
        component: () => import('@/views/customer/CustomerListView.vue'),
        meta: {
          title: '客户管理',
          roles: [ADMIN, SERVICE, CUSTOMER],
          menu: { icon: 'OfficeBuilding' },
          breadcrumb: ['首页', '客户管理']
        }
      },
      {
        path: 'devices',
        name: 'device-list',
        component: () => import('@/views/device/DeviceListView.vue'),
        meta: {
          title: '设备档案',
          roles: [ADMIN, SERVICE, ENGINEER, CUSTOMER],
          menu: { icon: 'Cpu' },
          breadcrumb: ['首页', '设备档案']
        }
      },
      {
        path: 'evaluations',
        name: 'evaluation',
        component: () => import('@/views/EvaluationView.vue'),
        meta: { title: '服务评价', menu: { icon: 'Star' }, breadcrumb: ['首页', '服务评价'] }
      },
      {
        path: 'system/users',
        name: 'system-users',
        component: () => import('@/views/system/UserListView.vue'),
        meta: {
          title: '用户管理',
          roles: [ADMIN],
          menu: { icon: 'User' },
          breadcrumb: ['首页', '系统管理', '用户管理']
        }
      },
      {
        path: 'system/roles',
        name: 'system-roles',
        component: () => import('@/views/system/RoleListView.vue'),
        meta: {
          title: '角色与权限',
          roles: [ADMIN],
          menu: { icon: 'Lock' },
          breadcrumb: ['首页', '系统管理', '角色与权限']
        }
      },
      {
        path: 'system/configs',
        name: 'system-configs',
        component: () => import('@/views/system/ConfigView.vue'),
        meta: {
          title: '系统配置',
          roles: [ADMIN],
          menu: { icon: 'Setting' },
          breadcrumb: ['首页', '系统管理', '系统配置']
        }
      },
      {
        path: 'system/logs',
        name: 'system-logs',
        component: () => import('@/views/system/LogListView.vue'),
        meta: {
          title: '操作日志',
          roles: [ADMIN],
          menu: { icon: 'Document' },
          breadcrumb: ['首页', '系统管理', '操作日志']
        }
      },
      {
        path: 'profile',
        name: 'profile',
        component: () => import('@/views/ProfileView.vue'),
        meta: { title: '个人中心', breadcrumb: ['首页', '个人中心'] }
      }
    ]
  },
  {
    path: '/403',
    name: 'forbidden',
    component: () => import('@/views/ForbiddenView.vue'),
    meta: { title: '无访问权限', public: true }
  },
  {
    path: '/:pathMatch(.*)*',
    name: 'not-found',
    component: () => import('@/views/NotFoundView.vue'),
    meta: { title: '页面不存在', public: true }
  }
]

/** 根据角色过滤出可见菜单（仅用于界面展示，后端仍会强制校验） */
export function buildMenus(authStore) {
  const layoutRoute = routes.find(route => route.path === '/')
  return (layoutRoute?.children || [])
    .filter(route => route.meta?.menu)
    .filter(route => !route.meta.roles || route.meta.roles.some(role => authStore.hasRole(role)))
    .map(route => ({
      index: `/${route.path}`,
      title: route.meta.title,
      icon: route.meta.menu.icon
    }))
}

const router = createRouter({
  history: createWebHistory(),
  routes
})

router.beforeEach(async to => {
  const authStore = useAuthStore()
  if (to.meta?.public) {
    if (to.name === 'login' && authStore.isLoggedIn) {
      return { path: '/dashboard' }
    }
    return true
  }
  if (!authStore.isLoggedIn) {
    return { path: '/login', query: { redirect: to.fullPath } }
  }
  if (!authStore.user) {
    try {
      await authStore.loadCurrentUser()
    } catch {
      authStore.clear()
      return { path: '/login', query: { redirect: to.fullPath } }
    }
  }
  const allowedRoles = to.meta?.roles
  if (allowedRoles && allowedRoles.length > 0 && !allowedRoles.some(role => authStore.hasRole(role))) {
    return { path: '/403' }
  }
  return true
})

export default router
