import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import { enableAutoUnmount, flushPromises, mount } from '@vue/test-utils'
import ElementPlus from 'element-plus'

import DashboardView from '@/views/DashboardView.vue'
import { fetchHealth } from '@/api/health'

// Element Plus 组件挂载/卸载依赖真实 DOM 生命周期，自动卸载可避免用例之间互相污染
enableAutoUnmount(afterEach)

vi.mock('@/api/health', () => ({
  fetchHealth: vi.fn()
}))

const mockedFetchHealth = vi.mocked(fetchHealth)

function mountDashboard() {
  return mount(DashboardView, {
    // Element Plus 组件在挂载钩子中访问真实 DOM 引用，jsdom 下需 attachTo 才能正常工作
    attachTo: document.body,
    global: {
      plugins: [ElementPlus]
    }
  })
}

describe('DashboardView', () => {
  beforeEach(() => {
    mockedFetchHealth.mockReset()
  })

  it('渲染后端返回的真实健康状态', async () => {
    mockedFetchHealth.mockResolvedValue({
      application: 'serviceflow',
      status: 'UP',
      time: '2026-10-15 10:30:00',
      database: 'UP',
      redis: 'UP'
    })

    const wrapper = mountDashboard()
    await flushPromises()

    expect(mockedFetchHealth).toHaveBeenCalledTimes(1)
    const text = wrapper.text()
    expect(text).toContain('serviceflow')
    expect(text).toContain('MySQL UP')
    expect(text).toContain('Redis UP')
    expect(text).toContain('2026-10-15 10:30:00')
  })

  it('请求失败时展示错误提示而不是假数据', async () => {
    mockedFetchHealth.mockRejectedValue(new Error('网络异常，无法连接服务器'))

    const wrapper = mountDashboard()
    await flushPromises()

    expect(wrapper.text()).toContain('网络异常，无法连接服务器')
    expect(wrapper.text()).not.toContain('MySQL UP')
  })

  it('点击刷新按钮会重新请求接口', async () => {
    mockedFetchHealth.mockResolvedValue({
      application: 'serviceflow',
      status: 'UP',
      time: '2026-10-15 10:30:00',
      database: 'UP',
      redis: 'UP'
    })

    const wrapper = mountDashboard()
    await flushPromises()

    await wrapper.get('button').trigger('click')
    await flushPromises()

    expect(mockedFetchHealth).toHaveBeenCalledTimes(2)
  })
})
