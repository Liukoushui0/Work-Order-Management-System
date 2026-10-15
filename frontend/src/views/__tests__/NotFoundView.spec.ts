import { describe, expect, it, vi } from 'vitest'
import { mount } from '@vue/test-utils'
import ElementPlus from 'element-plus'

import NotFoundView from '@/views/NotFoundView.vue'

const push = vi.fn()

vi.mock('vue-router', () => ({
  useRouter: () => ({ push })
}))

describe('NotFoundView', () => {
  it('展示 404 提示并可返回仪表盘', async () => {
    const wrapper = mount(NotFoundView, {
      global: { plugins: [ElementPlus] }
    })

    expect(wrapper.text()).toContain('404')
    expect(wrapper.text()).toContain('你访问的页面不存在')

    await wrapper.find('button').trigger('click')

    expect(push).toHaveBeenCalledWith('/dashboard')
  })
})
