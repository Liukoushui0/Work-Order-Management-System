import { describe, expect, it } from 'vitest'
import { AxiosError } from 'axios'

import { ApiError, normalizeError } from '@/utils/request'

function axiosErrorWithResponse(status, data) {
  const error = new AxiosError('Request failed', 'ERR_BAD_REQUEST')
  // 构造与 axios 运行时一致的响应结构
  error.response = {
    status,
    statusText: '',
    headers: {},
    config: {},
    data
  }
  return error
}

describe('normalizeError', () => {
  it('优先使用后端统一响应体中的业务错误码与提示', () => {
    const error = axiosErrorWithResponse(404, { code: 1401, message: '工单不存在' })

    const result = normalizeError(error)

    expect(result).toBeInstanceOf(ApiError)
    expect(result.code).toBe(1401)
    expect(result.message).toBe('工单不存在')
    expect(result.httpStatus).toBe(404)
  })

  it('无响应体时给出 HTTP 状态提示', () => {
    const result = normalizeError(axiosErrorWithResponse(500))

    expect(result.code).toBe(-3)
    expect(result.message).toBe('请求失败（HTTP 500）')
  })

  it('超时错误给出超时提示', () => {
    const error = new AxiosError('timeout of 15000ms exceeded', 'ECONNABORTED')

    expect(normalizeError(error).message).toBe('请求超时，请稍后重试')
  })

  it('无响应对象时识别为网络异常', () => {
    const error = new AxiosError('Network Error', 'ERR_NETWORK')

    expect(normalizeError(error).message).toBe('网络异常，无法连接服务器')
  })

  it('已是 ApiError 时原样返回，避免重复包装', () => {
    const apiError = new ApiError(1101, '无权访问该资源', 403)

    expect(normalizeError(apiError)).toBe(apiError)
  })

  it('未知异常返回兜底提示', () => {
    expect(normalizeError('boom').message).toBe('未知错误，请稍后重试')
  })
})
