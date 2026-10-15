import { defineStore } from 'pinia'

import { fetchCurrentUser, login as loginApi, logout as logoutApi } from '@/api/auth'

const TOKEN_STORAGE_KEY = 'serviceflow_token'
const REFRESH_STORAGE_KEY = 'serviceflow_refresh_token'

/**
 * 认证状态：令牌、当前用户、角色与权限。
 * 权限判断只用于界面展示，真正的权限校验始终在后端执行。
 */
export const useAuthStore = defineStore('auth', {
  state: () => ({
    token: localStorage.getItem(TOKEN_STORAGE_KEY) || '',
    refreshToken: localStorage.getItem(REFRESH_STORAGE_KEY) || '',
    user: null,
    loading: false
  }),
  getters: {
    isLoggedIn: state => Boolean(state.token),
    roles: state => state.user?.roles || [],
    permissions: state => state.user?.permissions || [],
    displayName: state => state.user?.realName || state.user?.username || '',
    isCustomer: state => (state.user?.roles || []).includes('ROLE_CUSTOMER'),
    isEngineer: state => (state.user?.roles || []).includes('ROLE_ENGINEER'),
    isAdmin: state => (state.user?.roles || []).includes('ROLE_ADMIN'),
    isService: state => (state.user?.roles || []).includes('ROLE_SERVICE')
  },
  actions: {
    hasPermission(permCode) {
      return this.permissions.includes(permCode)
    },
    hasRole(roleCode) {
      return this.roles.includes(roleCode)
    },
    async login(credentials) {
      this.loading = true
      try {
        const data = await loginApi(credentials)
        this.setTokens(data.accessToken, data.refreshToken)
        this.user = data.user
        return data
      } finally {
        this.loading = false
      }
    },
    setTokens(accessToken, refreshToken) {
      this.token = accessToken || ''
      this.refreshToken = refreshToken || ''
      if (accessToken) {
        localStorage.setItem(TOKEN_STORAGE_KEY, accessToken)
      } else {
        localStorage.removeItem(TOKEN_STORAGE_KEY)
      }
      if (refreshToken) {
        localStorage.setItem(REFRESH_STORAGE_KEY, refreshToken)
      } else {
        localStorage.removeItem(REFRESH_STORAGE_KEY)
      }
    },
    async loadCurrentUser() {
      if (!this.token) return null
      this.user = await fetchCurrentUser()
      return this.user
    },
    async logout() {
      try {
        if (this.token) {
          await logoutApi()
        }
      } finally {
        this.clear()
      }
    },
    clear() {
      this.setTokens('', '')
      this.user = null
    }
  }
})
