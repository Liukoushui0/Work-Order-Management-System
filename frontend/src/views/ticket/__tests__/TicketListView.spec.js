import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import { enableAutoUnmount, flushPromises, mount } from '@vue/test-utils'
import ElementPlus from 'element-plus'
import { createPinia, setActivePinia } from 'pinia'

import TicketListView from '@/views/ticket/TicketListView.vue'
import { fetchTickets } from '@/api/ticket'

enableAutoUnmount(afterEach)

const push = vi.fn()

vi.mock('vue-router', () => ({
  useRouter: () => ({ push }),
  useRoute: () => ({ params: {} })
}))

vi.mock('element-plus', async importOriginal => {
  const actual = await importOriginal()
  return { ...actual, ElMessage: { success: vi.fn(), error: vi.fn(), warning: vi.fn() } }
})

vi.mock('@/api/ticket', () => ({
  fetchTickets: vi.fn(),
  fetchTicketDetail: vi.fn(),
  fetchOverdueTickets: vi.fn(),
  fetchTicketRecords: vi.fn(),
  createTicket: vi.fn(),
  assignTicket: vi.fn(),
  acceptTicket: vi.fn(),
  transferTicket: vi.fn(),
  rejectTicket: vi.fn(),
  processTicket: vi.fn(),
  completeTicket: vi.fn(),
  confirmTicket: vi.fn(),
  reopenTicket: vi.fn(),
  cancelTicket: vi.fn(),
  fetchComments: vi.fn(),
  createComment: vi.fn(),
  fetchAttachments: vi.fn(),
  uploadAttachment: vi.fn(),
  deleteAttachment: vi.fn(),
  fetchEvaluation: vi.fn(),
  createEvaluation: vi.fn()
}))

function mountList() {
  return mount(TicketListView, {
    attachTo: document.body,
    global: { plugins: [ElementPlus] }
  })
}

describe('TicketListView', () => {
  beforeEach(() => {
    setActivePinia(createPinia())
    push.mockClear()
    vi.mocked(fetchTickets).mockReset()
    vi.mocked(fetchTickets).mockResolvedValue({
      records: [
        {
          id: 8,
          ticketNo: 'SO20260101000008',
          title: '分拣终端急停按钮失灵',
          customerName: '星海智造有限公司',
          deviceName: '智能分拣终端 A2',
          priority: 'URGENT',
          status: 'PENDING_ACCEPT',
          assigneeName: '维修工程师二号',
          deadlineTime: '2026-10-14 20:00:00',
          overdue: true,
          evaluationScore: null
        }
      ],
      total: 1,
      pageNum: 1,
      pageSize: 10
    })
  })

  it('渲染后端返回的工单数据与逾期标记', async () => {
    const wrapper = mountList()
    await flushPromises()

    const text = wrapper.text()
    expect(text).toContain('SO20260101000008')
    expect(text).toContain('分拣终端急停按钮失灵')
    expect(text).toContain('星海智造有限公司')
    expect(text).toContain('紧急')
    expect(text).toContain('待接单')
    expect(text).toContain('逾期')
    expect(text).toContain('未评价')
  })

  it('输入关键词点击查询会带上筛选条件重新请求', async () => {
    const wrapper = mountList()
    await flushPromises()
    vi.mocked(fetchTickets).mockClear()

    const keywordInput = wrapper.find('input[placeholder="工单编号 / 标题 / 报修人"]')
    await keywordInput.setValue('急停')
    await wrapper.findAll('button').find(button => button.text() === '查询').trigger('click')
    await flushPromises()

    expect(vi.mocked(fetchTickets)).toHaveBeenCalledWith(
      expect.objectContaining({ keyword: '急停', pageNum: 1 })
    )
  })

  it('查询失败时清空列表且不展示旧数据', async () => {
    vi.mocked(fetchTickets).mockRejectedValue(new Error('无权访问该资源'))

    const wrapper = mountList()
    await flushPromises()

    expect(wrapper.text()).toContain('没有符合条件的工单')
    expect(wrapper.text()).not.toContain('SO20260101000008')
  })

  it('点击详情跳转到工单详情页', async () => {
    const wrapper = mountList()
    await flushPromises()

    await wrapper.findAll('button').find(button => button.text() === '详情').trigger('click')

    expect(push).toHaveBeenCalledWith('/tickets/8')
  })
})
