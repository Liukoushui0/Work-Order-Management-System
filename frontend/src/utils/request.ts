import axios, { type AxiosError, type AxiosInstance, type AxiosResponse } from 'axios'

import type { ApiResult } from '@/types/api'

const TOKEN_STORAGE_KEY = 'serviceflow_token'

/** 业务/网络错误统一封装，供页面展示与测试断言使用 */
export class ApiError extends Error {
  readonly code: number
  readonly httpStatus?: number

  constructor(code: number, message: string, httpStatus?: number) {
    super(message)
    this.name = 'ApiError'
    this.code = code
    this.httpStatus = httpStatus
  }
}

/**
 * 把任意异常归一化为 ApiError：
 * 优先使用后端统一响应体中的 code/message，其次区分超时、网络错误与未知错误。
 */
export function normalizeError(error: unknown): ApiError {
  if (error instanceof ApiError) {
    return error
  }
  if (axios.isAxiosError(error)) {
    const axiosError = error as AxiosError<ApiResult<unknown>>
    const httpStatus = axiosError.response?.status
    const payload = axiosError.response?.data
    if (payload && typeof payload.code === 'number') {
      return new ApiError(payload.code, payload.message || '请求失败', httpStatus)
    }
    if (axiosError.code === 'ECONNABORTED' || axiosError.code === 'ETIMEDOUT') {
      return new ApiError(-1, '请求超时，请稍后重试', httpStatus)
    }
    if (!axiosError.response) {
      return new ApiError(-2, '网络异常，无法连接服务器', httpStatus)
    }
    return new ApiError(-3, `请求失败（HTTP ${httpStatus}）`, httpStatus)
  }
  if (error instanceof Error) {
    return new ApiError(-98, error.message)
  }
  return new ApiError(-99, '未知错误，请稍后重试')
}

const http: AxiosInstance = axios.create({
  baseURL: import.meta.env.VITE_API_BASE_URL || '/api/v1',
  timeout: 15000,
  headers: { 'Content-Type': 'application/json' }
})

http.interceptors.request.use(config => {
  const token = localStorage.getItem(TOKEN_STORAGE_KEY)
  if (token) {
    config.headers.Authorization = `Bearer ${token}`
  }
  return config
})

http.interceptors.response.use(
  (response: AxiosResponse<ApiResult<unknown>>) => {
    const payload = response.data
    if (payload && typeof payload.code === 'number' && payload.code !== 0) {
      return Promise.reject(new ApiError(payload.code, payload.message || '请求失败', response.status))
    }
    return response
  },
  error => Promise.reject(normalizeError(error))
)

export async function get<T>(url: string, params?: Record<string, unknown>): Promise<T> {
  const response = await http.get<ApiResult<T>>(url, { params })
  return response.data.data as T
}

export async function post<T>(url: string, body?: unknown): Promise<T> {
  const response = await http.post<ApiResult<T>>(url, body)
  return response.data.data as T
}

export async function put<T>(url: string, body?: unknown): Promise<T> {
  const response = await http.put<ApiResult<T>>(url, body)
  return response.data.data as T
}

export async function del<T>(url: string): Promise<T> {
  const response = await http.delete<ApiResult<T>>(url)
  return response.data.data as T
}

export default http
