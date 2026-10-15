import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import { enableAutoUnmount, flushPromises, mount } from '@vue/test-utils'
import ElementPlus from 'element-plus'
import { createPinia, setActivePinia } from 'pinia'

import DashboardView from '@/views/DashboardView.vue'
import {
  fetchEngineerWorkload,
  fetchFaultDistribution,
  fetchOverview,
  fetchPriorityDistribution,
  fetchSatisfaction,
  fetchTrend
} from '@/api/dashboard'

enableAutoUnmount(afterEach)

vi.mock('element-plus', async importOriginal => {
  const actual = await importOriginal()
  return { ...actual, ElMessage: { success: vi.fn(), error: vi.fn(), warning: vi.fn() } }
})

vi.mock('@/api/dashboard', () => ({
  fetchOverview: vi.fn(),
  fetchTrend: vi.fn(),
  fetchFaultDistribution: vi.fn(),
  fetchPriorityDistribution: vi.fn(),
  fetchEngineerWorkload: vi.fn(),
  fetchSatisfaction: vi.fn()
}))

function mountDashboard() {
  return mount(DashboardView, {
    attachTo: document.body,
    global: {
      plugins: [ElementPlus],
      stubs: { BaseChart: { template: '<div class="chart-stub" />' } }
    }
  })
}

describe('DashboardView', () => {
  beforeEach(() => {
    setActivePinia(createPinia())
    vi.mocked(fetchOverview).mockResolvedValue({
      total: 12,
      openCount: 5,
      overdueCount: 2,
      completedCount: 7,
      avgHandleHours: 18.5,
      avgSatisfaction: 4.5,
      scope: 'ALL',
      cached: false,
      statusDistribution: []
    })
    vi.mocked(fetchTrend).mockResolvedValue([
      { period: '2026-10-14', created: 2, completed: 1 },
      { period: '2026-10-15', created: 3, completed: 2 }
    ])
    vi.mocked(fetchFaultDistribution).mockResolvedValue([{ key: '1', name: '硬件故障', value: 6, percent: 50 }])
    vi.mocked(fetchPriorityDistribution).mockResolvedValue([{ key: 'HIGH', name: '高', value: 4, percent: 33.3 }])
    vi.mocked(fetchEngineerWorkload).mockResolvedValue([
      { engineerId: 3, engineerName: '维修工程师一号', total: 3, completed: 2, open: 1, avgHandleHours: 6 }
    ])
    vi.mocked(fetchSatisfaction).mockResolvedValue({
      total: 2,
      avgScore: 4.5,
      distribution: [{ key: '5', name: '5 星', value: 1, percent: 50 }]
    })
  })

  it('使用后端真实统计渲染指标卡与图表', async () => {
    const wrapper = mountDashboard()
    await flushPromises()

    expect(fetchOverview).toHaveBeenCalledTimes(1)
    const text = wrapper.text()
    expect(text).toContain('工单总数')
    expect(text).toContain('12')
    expect(text).toContain('18.5 h')
    expect(text).toContain('4.5 分')
    expect(wrapper.findAll('.chart-stub').length).toBeGreaterThan(0)
  })

  it('切换趋势粒度会重新请求趋势接口', async () => {
    const wrapper = mountDashboard()
    await flushPromises()
    vi.mocked(fetchTrend).mockClear()

    const monthlyRadio = wrapper.findAll('.el-radio-button').at(2)
    await monthlyRadio.trigger('click')
    await flushPromises()

    expect(fetchTrend).toHaveBeenCalledWith({ granularity: 'MONTH' })
  })

  it('统计接口失败时展示错误提示而不是假数据', async () => {
    vi.mocked(fetchOverview).mockRejectedValue(new Error('网络异常，无法连接服务器'))

    const wrapper = mountDashboard()
    await flushPromises()

    expect(wrapper.text()).toContain('网络异常，无法连接服务器')
    expect(wrapper.text()).not.toContain('工单总数')
  })
})
