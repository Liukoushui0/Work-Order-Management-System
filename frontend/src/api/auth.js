import { get, post, put } from '@/utils/request'

/** 登录，返回 access/refresh token 与用户信息 */
export function login(data) {
  return post('/auth/login', data)
}

/** 退出登录（令牌加入黑名单） */
export function logout() {
  return post('/auth/logout')
}

/** 当前用户信息（含角色与权限） */
export function fetchCurrentUser() {
  return get('/auth/me')
}

/** 修改密码 */
export function changePassword(data) {
  return put('/auth/password', data)
}

/** 更新个人资料 */
export function updateProfile(data) {
  return put('/auth/profile', data)
}
