import axios from 'axios'

const TOKEN_STORAGE_KEY = 'serviceflow_token'

/**
 * 业务/网络错误统一封装，供页面展示与测试断言使用。
 */
export class ApiError extends Error {
  /**
   * @param {number} code 业务错误码；负数表示本地产生的错误（超时、网络异常等）
   * @param {string} message 面向用户的提示信息
   * @param {number} [httpStatus] HTTP 状态码
   */
  constructor(code, message, httpStatus) {
    super(message)
    this.name = 'ApiError'
    this.code = code
    this.httpStatus = httpStatus
  }
}

/**
 * 把任意异常归一化为 ApiError：
 * 优先使用后端统一响应体中的 code/message，其次区分超时、网络错误与未知错误。
 *
 * @param {unknown} error 任意异常
 * @returns {ApiError} 归一化后的错误对象
 */
export function normalizeError(error) {
  if (error instanceof ApiError) {
    return error
  }
  if (axios.isAxiosError(error)) {
    const httpStatus = error.response?.status
    const payload = error.response?.data
    if (payload && typeof payload.code === 'number') {
      return new ApiError(payload.code, payload.message || '请求失败', httpStatus)
    }
    if (error.code === 'ECONNABORTED' || error.code === 'ETIMEDOUT') {
      return new ApiError(-1, '请求超时，请稍后重试', httpStatus)
    }
    if (!error.response) {
      return new ApiError(-2, '网络异常，无法连接服务器', httpStatus)
    }
    return new ApiError(-3, `请求失败（HTTP ${httpStatus}）`, httpStatus)
  }
  if (error instanceof Error) {
    return new ApiError(-98, error.message)
  }
  return new ApiError(-99, '未知错误，请稍后重试')
}

const http = axios.create({
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
  response => {
    const payload = response.data
    if (payload && typeof payload.code === 'number' && payload.code !== 0) {
      return Promise.reject(new ApiError(payload.code, payload.message || '请求失败', response.status))
    }
    return response
  },
  error => Promise.reject(normalizeError(error))
)

/**
 * GET 请求，解包统一响应结构后返回 data 字段。
 * @param {string} url
 * @param {Record<string, unknown>} [params]
 * @returns {Promise<any>}
 */
export async function get(url, params) {
  const response = await http.get(url, { params })
  return response.data.data
}

/**
 * POST 请求，解包统一响应结构后返回 data 字段。
 * @param {string} url
 * @param {unknown} [body]
 * @returns {Promise<any>}
 */
export async function post(url, body) {
  const response = await http.post(url, body)
  return response.data.data
}

/**
 * PUT 请求，解包统一响应结构后返回 data 字段。
 * @param {string} url
 * @param {unknown} [body]
 * @returns {Promise<any>}
 */
export async function put(url, body) {
  const response = await http.put(url, body)
  return response.data.data
}

/**
 * DELETE 请求，解包统一响应结构后返回 data 字段。
 * @param {string} url
 * @returns {Promise<any>}
 */
export async function del(url) {
  const response = await http.delete(url)
  return response.data.data
}

export default http
