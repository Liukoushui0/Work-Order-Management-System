<script setup>
import { reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'

import { useAuthStore } from '@/stores/auth'
import { normalizeError } from '@/utils/request'

const router = useRouter()
const route = useRoute()
const authStore = useAuthStore()
const formRef = ref(null)
const submitting = ref(false)
const errorMessage = ref('')
/** 演示账号仅在开发环境或显式开启时展示，避免生产构建暴露本地开发账号 */
const demoEnabled = import.meta.env.DEV || import.meta.env.VITE_ENABLE_DEMO_ACCOUNTS === 'true'

const form = reactive({
  username: '',
  password: ''
})

const rules = {
  username: [{ required: true, message: '请输入登录账号', trigger: 'blur' }],
  password: [
    { required: true, message: '请输入登录密码', trigger: 'blur' },
    { min: 6, message: '密码长度至少 6 位', trigger: 'blur' }
  ]
}

const demoAccounts = [
  { username: 'admin', label: '系统管理员' },
  { username: 'service01', label: '售后客服' },
  { username: 'engineer01', label: '维修工程师' },
  { username: 'customer01', label: '客户' }
]

function useDemoAccount(account) {
  form.username = account.username
  form.password = 'Serviceflow@2026'
}

async function submit() {
  errorMessage.value = ''
  // 先做一次显式校验，保证必填与长度限制立即给出反馈（表单校验仍保留用于逐项提示）
  if (!form.username.trim()) {
    ElMessage.warning('请输入登录账号')
    return
  }
  if (!form.password) {
    ElMessage.warning('请输入登录密码')
    return
  }
  if (form.password.length < 6) {
    ElMessage.warning('密码长度至少 6 位')
    return
  }
  const valid = await formRef.value.validate().catch(() => false)
  if (!valid) return
  submitting.value = true
  try {
    await authStore.login({ username: form.username, password: form.password })
    const redirect = typeof route.query.redirect === 'string' ? route.query.redirect : '/dashboard'
    ElMessage.success('登录成功')
    router.push(redirect)
  } catch (error) {
    errorMessage.value = normalizeError(error).message
    ElMessage.error(errorMessage.value)
  } finally {
    submitting.value = false
  }
}
</script>

<template>
  <div class="login-page">
    <div class="login-card">
      <div class="login-card__intro">
        <div class="login-card__logo">SF</div>
        <h1>ServiceFlow</h1>
        <p>企业售后工单管理系统</p>
        <ul>
          <li>报修 → 建单 → 分派 → 维修 → 确认 → 评价，全流程可追溯</li>
          <li>四种角色后端强制鉴权，数据按客户与负责人隔离</li>
          <li>运营统计与 SLA 逾期监控</li>
        </ul>
      </div>

      <div class="login-card__form">
        <h2>账号登录</h2>
        <el-alert v-if="errorMessage" type="error" :title="errorMessage" :closable="false" show-icon
                  class="login-card__alert" />
        <el-form ref="formRef" :model="form" :rules="rules" label-position="top" @keyup.enter="submit">
          <el-form-item label="登录账号" prop="username">
            <el-input v-model="form.username" placeholder="请输入登录账号" clearable>
              <template #prefix><el-icon><User /></el-icon></template>
            </el-input>
          </el-form-item>
          <el-form-item label="登录密码" prop="password">
            <el-input v-model="form.password" type="password" placeholder="请输入登录密码" show-password>
              <template #prefix><el-icon><Lock /></el-icon></template>
            </el-input>
          </el-form-item>
          <el-button type="primary" class="login-card__submit" :loading="submitting" @click="submit">
            登录
          </el-button>
        </el-form>

        <div v-if="demoEnabled" class="login-card__demo-title">
          <span>演示账号</span>
          <span class="login-card__demo-password">密码 Serviceflow@2026</span>
        </div>
        <div v-if="demoEnabled" class="login-card__demo">
          <el-button v-for="account in demoAccounts" :key="account.username" size="small"
                     @click="useDemoAccount(account)">
            {{ account.label }}
          </el-button>
        </div>
      </div>
    </div>
  </div>
</template>

<style scoped>
.login-page {
  min-height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  background:
    radial-gradient(circle at 12% 18%, rgba(43, 108, 255, 0.16) 0%, transparent 42%),
    radial-gradient(circle at 88% 82%, rgba(28, 79, 214, 0.14) 0%, transparent 46%),
    linear-gradient(135deg, #eef4ff 0%, #f7f9ff 55%, #eaf1ff 100%);
  padding: 24px;
}

.login-card {
  display: flex;
  width: 900px;
  max-width: 100%;
  min-height: 470px;
  background: #fff;
  border-radius: 16px;
  box-shadow:
    0 24px 60px rgba(16, 24, 40, 0.1),
    0 2px 6px rgba(43, 108, 255, 0.08);
  overflow: hidden;
}

.login-card__intro {
  flex: 1;
  padding: 48px 40px;
  background:
    radial-gradient(circle at 88% 12%, rgba(255, 255, 255, 0.22) 0%, transparent 40%),
    linear-gradient(160deg, #2b6cff 0%, #1c4fd6 100%);
  color: #fff;
}

.login-card__logo {
  width: 44px;
  height: 44px;
  border-radius: 12px;
  background: rgba(255, 255, 255, 0.2);
  border: 1px solid rgba(255, 255, 255, 0.28);
  display: flex;
  align-items: center;
  justify-content: center;
  font-weight: 700;
  margin-bottom: 20px;
}

.login-card__intro h1 {
  margin: 0 0 6px;
  font-size: 26px;
  letter-spacing: 0.5px;
}

.login-card__intro p {
  margin: 0 0 28px;
  opacity: 0.86;
}

.login-card__intro ul {
  padding-left: 18px;
  margin: 0;
  line-height: 2;
  font-size: 13px;
  opacity: 0.92;
}

.login-card__form {
  flex: 1;
  padding: 48px 40px;
}

.login-card__form h2 {
  margin: 0 0 20px;
  font-size: 20px;
  color: var(--sf-text-primary);
}

.login-card__alert {
  margin-bottom: 12px;
}

.login-card__submit {
  width: 100%;
  margin-top: 4px;
  height: 42px;
  font-size: 15px;
  letter-spacing: 2px;
}

.login-card__demo {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.login-card__demo-title {
  display: flex;
  align-items: center;
  gap: 10px;
  margin: 18px 0 10px;
  font-size: 12px;
  color: var(--sf-text-secondary);
  white-space: nowrap;
}

.login-card__demo-title::before,
.login-card__demo-title::after {
  content: '';
  flex: 1;
  height: 1px;
  background: var(--sf-border-color);
}

.login-card__demo-password {
  color: var(--sf-text-primary);
  font-weight: 500;
  font-family: Consolas, Monaco, monospace;
}

@media (max-width: 860px) {
  .login-card {
    flex-direction: column;
  }

  .login-card__intro,
  .login-card__form {
    padding: 32px 24px;
  }
}
</style>
