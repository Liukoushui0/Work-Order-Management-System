import { get } from '@/utils/request'
import type { HealthStatus } from '@/types/api'

/** 查询应用健康状态（应用、数据库、Redis） */
export function fetchHealth(): Promise<HealthStatus> {
  return get<HealthStatus>('/health')
}
