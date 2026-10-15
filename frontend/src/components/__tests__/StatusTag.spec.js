import { describe, expect, it } from 'vitest'
import { mount } from '@vue/test-utils'
import ElementPlus from 'element-plus'

import StatusTag from '@/components/StatusTag.vue'

function mountTag(props) {
  return mount(StatusTag, {
    props,
    global: { plugins: [ElementPlus] }
  })
}

describe('StatusTag', () => {
  it('按状态渲染中文标签', () => {
    expect(mountTag({ value: 'PENDING_ASSIGN' }).text()).toBe('待分派')
    expect(mountTag({ value: 'PROCESSING' }).text()).toBe('处理中')
    expect(mountTag({ value: 'COMPLETED' }).text()).toBe('已完成')
  })

  it('按优先级渲染中文标签', () => {
    expect(mountTag({ kind: 'priority', value: 'URGENT' }).text()).toBe('紧急')
    expect(mountTag({ kind: 'priority', value: 'LOW' }).text()).toBe('低')
  })

  it('未知取值时回退展示原始值', () => {
    expect(mountTag({ value: 'UNKNOWN_STATUS' }).text()).toBe('UNKNOWN_STATUS')
  })
})
