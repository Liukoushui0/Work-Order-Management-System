import { createRouter, createWebHistory } from 'vue-router'

import MainLayout from '@/layouts/MainLayout.vue'

/**
 * 路由表：
 * - 当前阶段包含应用外壳、仪表盘与 404；
 * - 登录页、业务页面与基于角色的路由守卫在后续阶段随对应后端模块一并接入。
 */
export const routes = [
  {
    path: '/',
    component: MainLayout,
    redirect: '/dashboard',
    children: [
      {
        path: 'dashboard',
        name: 'dashboard',
        component: () => import('@/views/DashboardView.vue'),
        meta: { title: '售后运营仪表盘', breadcrumb: ['首页', '售后运营仪表盘'] }
      }
    ]
  },
  {
    path: '/:pathMatch(.*)*',
    name: 'not-found',
    component: () => import('@/views/NotFoundView.vue'),
    meta: { title: '页面不存在' }
  }
]

const router = createRouter({
  history: createWebHistory(),
  routes
})

export default router
