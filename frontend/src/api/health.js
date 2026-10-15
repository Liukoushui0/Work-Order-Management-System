import { get } from '@/utils/request'

/**
 * @typedef {Object} HealthStatus
 * @property {string} application 应用名称
 * @property {string} status 应用状态
 * @property {string} time 服务器时间
 * @property {string} database 数据库状态
 * @property {string} redis 缓存状态
 */

/**
 * 查询应用健康状态（应用、数据库、Redis）。
 * @returns {Promise<HealthStatus>}
 */
export function fetchHealth() {
  return get('/health')
}
