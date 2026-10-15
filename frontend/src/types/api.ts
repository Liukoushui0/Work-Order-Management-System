/** 后端统一响应结构，对应 backend 的 com.serviceflow.common.api.Result */
export interface ApiResult<T> {
  code: number
  message: string
  data?: T
  traceId?: string
  timestamp?: string
}

/** 统一分页响应结构，对应 backend 的 PageResult */
export interface PageResult<T> {
  records: T[]
  total: number
  pageNum: number
  pageSize: number
  pages: number
}

/** 通用分页查询参数 */
export interface PageQuery {
  pageNum?: number
  pageSize?: number
  sortField?: string
  sortOrder?: 'asc' | 'desc'
}

/** 健康检查响应 */
export interface HealthStatus {
  application: string
  status: string
  time: string
  database: string
  redis: string
}
