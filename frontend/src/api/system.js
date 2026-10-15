import { del, get, post, put } from '@/utils/request'

/** 用户管理 */
export function fetchUsers(params) {
  return get('/users', params)
}

export function createUser(data) {
  return post('/users', data)
}

export function updateUser(id, data) {
  return put(`/users/${id}`, data)
}

export function updateUserStatus(id, status) {
  return put(`/users/${id}/status`, { status })
}

export function assignUserRoles(id, roleIds) {
  return put(`/users/${id}/roles`, { roleIds })
}

export function resetUserPassword(id, newPassword) {
  return post(`/users/${id}/reset-password`, { newPassword })
}

export function fetchRoles() {
  return get('/roles')
}

/** 可指派工程师（需要分派或转派权限） */
export function fetchEngineers() {
  return get('/users/engineers')
}

export function fetchPermissions() {
  return get('/permissions')
}

/** 故障分类 */
export function fetchFaultCategories(params) {
  return get('/fault-categories', params)
}

export function fetchEnabledFaultCategories() {
  return get('/fault-categories/enabled')
}

export function createFaultCategory(data) {
  return post('/fault-categories', data)
}

export function updateFaultCategory(id, data) {
  return put(`/fault-categories/${id}`, data)
}

export function deleteFaultCategory(id) {
  return del(`/fault-categories/${id}`)
}

/** 系统配置 */
export function fetchConfigs() {
  return get('/configs')
}

export function updateConfig(configKey, configValue) {
  return put(`/configs/${configKey}`, { configValue })
}

/** 操作日志 */
export function fetchOperationLogs(params) {
  return get('/operation-logs', params)
}
