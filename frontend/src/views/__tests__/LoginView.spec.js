import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import { enableAutoUnmount, flushPromises, mount } from '@vue/test-utils'
import ElementPlus from 'element-plus'
import { createPinia, setActivePinia } from 'pinia'

import LoginView from '@/views/LoginView.vue'
import { login } from '@/api/auth'
import { ApiError } from '@/utils/request'
import { ElMessage } from 'element-plus'

enableAutoUnmount(afterEach)

const push = vi.fn()

vi.mock('vue-router', () => ({
  useRouter: () => ({ push }),
  useRoute: () => ({ query: {} })
}))

vi.mock('element-plus', async importOriginal => {
  const actual = await importOriginal()
  return { ...actual, ElMessage: { success: vi.fn(), error: vi.fn(), warning: vi.fn() } }
})

vi.mock('@/api/auth', () => ({
  login: vi.fn(),
  logout: vi.fn(),
  fetchCurrentUser: vi.fn(),
  changePassword: vi.fn(),
  updateProfile: vi.fn()
}))

function mountLogin() {
  return mount(LoginView, {
    attachTo: document.body,
    global: { plugins: [ElementPlus] }
  })
}

async function fillAndSubmit(wrapper, username, password) {
  if (username !== null) {
    await wrapper.find('input[placeholder="请输入登录账号"]').setValue(username)
  }
  if (password !== null) {
    await wrapper.find('input[placeholder="请输入登录密码"]').setValue(password)
  }
  await wrapper.find('button.el-button--primary').trigger('click')
  await flushPromises()
}

describe('LoginView', () => {
  beforeEach(() => {
    setActivePinia(createPinia())
    localStorage.clear()
    push.mockClear()
    vi.mocked(login).mockReset()
    vi.mocked(ElMessage.warning).mockClear()
  })

  it('未填写账号密码时进行表单校验并阻止提交', async () => {
    const wrapper = mountLogin()

    await fillAndSubmit(wrapper, '', '')

    expect(vi.mocked(login)).not.toHaveBeenCalled()
    expect(vi.mocked(ElMessage.warning)).toHaveBeenCalledWith('请输入登录账号')
  })

  it('密码过短时提示长度限制', async () => {
    const wrapper = mountLogin()

    await fillAndSubmit(wrapper, 'admin', '123')

    expect(vi.mocked(login)).not.toHaveBeenCalled()
    expect(vi.mocked(ElMessage.warning)).toHaveBeenCalledWith('密码长度至少 6 位')
  })

  it('校验通过后调用登录接口、保存令牌并跳转默认页', async () => {
    vi.mocked(login).mockResolvedValue({
      accessToken: 'token-1',
      refreshToken: 'refresh-1',
      user: {
        id: 1,
        username: 'admin',
        realName: '系统管理员',
        roles: ['ROLE_ADMIN'],
        permissions: ['user:view']
      }
    })
    const wrapper = mountLogin()

    await fillAndSubmit(wrapper, 'admin', 'Serviceflow@2026')

    expect(vi.mocked(login)).toHaveBeenCalledWith({ username: 'admin', password: 'Serviceflow@2026' })
    expect(localStorage.getItem('serviceflow_token')).toBe('token-1')
    expect(push).toHaveBeenCalledWith('/dashboard')
  })

  it('登录失败时展示后端错误信息且不跳转', async () => {
    vi.mocked(login).mockRejectedValue(new ApiError(1001, '用户名或密码错误', 401))
    const wrapper = mountLogin()

    await fillAndSubmit(wrapper, 'admin', 'WrongPassword')

    expect(wrapper.text()).toContain('用户名或密码错误')
    expect(push).not.toHaveBeenCalled()
  })
})
