<script setup>
import { onMounted, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'

import { changePassword, updateProfile } from '@/api/auth'
import { useAuthStore } from '@/stores/auth'
import { normalizeError } from '@/utils/request'

const authStore = useAuthStore()
const profileForm = reactive({ realName: '', phone: '', email: '', avatar: '' })
const passwordForm = reactive({ oldPassword: '', newPassword: '', confirmPassword: '' })
const savingProfile = ref(false)
const savingPassword = ref(false)

const roleNameMap = {
  ROLE_ADMIN: '系统管理员',
  ROLE_SERVICE: '售后客服',
  ROLE_ENGINEER: '维修工程师',
  ROLE_CUSTOMER: '客户'
}

async function saveProfile() {
  if (!profileForm.realName.trim()) {
    ElMessage.warning('真实姓名不能为空')
    return
  }
  savingProfile.value = true
  try {
    await updateProfile({ ...profileForm })
    await authStore.loadCurrentUser()
    ElMessage.success('资料已更新')
  } catch (error) {
    ElMessage.error(normalizeError(error).message)
  } finally {
    savingProfile.value = false
  }
}

async function savePassword() {
  if (!passwordForm.oldPassword || !passwordForm.newPassword) {
    ElMessage.warning('请填写原密码与新密码')
    return
  }
  if (passwordForm.newPassword !== passwordForm.confirmPassword) {
    ElMessage.warning('两次输入的新密码不一致')
    return
  }
  if (passwordForm.newPassword.length < 8) {
    ElMessage.warning('新密码长度至少 8 位')
    return
  }
  savingPassword.value = true
  try {
    await changePassword({
      oldPassword: passwordForm.oldPassword,
      newPassword: passwordForm.newPassword
    })
    ElMessage.success('密码已修改，请重新登录')
    await authStore.logout()
    window.location.href = '/login'
  } catch (error) {
    ElMessage.error(normalizeError(error).message)
  } finally {
    savingPassword.value = false
  }
}

onMounted(async () => {
  if (!authStore.user) {
    await authStore.loadCurrentUser().catch(() => {})
  }
  const user = authStore.user || {}
  profileForm.realName = user.realName || ''
  profileForm.phone = user.phone || ''
  profileForm.email = user.email || ''
  profileForm.avatar = user.avatar || ''
})
</script>

<template>
  <el-row :gutter="16">
    <el-col :xs="24" :md="8">
      <el-card shadow="never">
        <template #header><span>账号信息</span></template>
        <el-descriptions :column="1" border>
          <el-descriptions-item label="登录账号">{{ authStore.user?.username }}</el-descriptions-item>
          <el-descriptions-item label="角色">
            <el-tag v-for="role in authStore.roles" :key="role" size="small" class="role-tag">
              {{ roleNameMap[role] || role }}
            </el-tag>
          </el-descriptions-item>
          <el-descriptions-item label="权限数量">{{ authStore.permissions.length }}</el-descriptions-item>
          <el-descriptions-item label="绑定客户">
            {{ authStore.user?.customerId || '未绑定' }}
          </el-descriptions-item>
        </el-descriptions>
      </el-card>
    </el-col>

    <el-col :xs="24" :md="16">
      <el-card shadow="never">
        <template #header><span>个人资料</span></template>
        <el-form :model="profileForm" label-width="100px">
          <el-form-item label="真实姓名">
            <el-input v-model="profileForm.realName" maxlength="50" />
          </el-form-item>
          <el-form-item label="手机号">
            <el-input v-model="profileForm.phone" maxlength="20" placeholder="11 位手机号" />
          </el-form-item>
          <el-form-item label="邮箱">
            <el-input v-model="profileForm.email" maxlength="100" />
          </el-form-item>
          <el-form-item label="头像地址">
            <el-input v-model="profileForm.avatar" maxlength="255" placeholder="可选" />
          </el-form-item>
          <el-form-item>
            <el-button type="primary" :loading="savingProfile" @click="saveProfile">保存资料</el-button>
          </el-form-item>
        </el-form>
      </el-card>

      <el-card shadow="never" class="password-card">
        <template #header><span>修改密码</span></template>
        <el-form :model="passwordForm" label-width="100px">
          <el-form-item label="原密码">
            <el-input v-model="passwordForm.oldPassword" type="password" show-password />
          </el-form-item>
          <el-form-item label="新密码">
            <el-input v-model="passwordForm.newPassword" type="password" show-password
                      placeholder="至少 8 位，需包含字母与数字" />
          </el-form-item>
          <el-form-item label="确认新密码">
            <el-input v-model="passwordForm.confirmPassword" type="password" show-password />
          </el-form-item>
          <el-form-item>
            <el-button type="primary" :loading="savingPassword" @click="savePassword">修改密码</el-button>
            <span class="hint">修改成功后当前令牌立即失效，需要重新登录</span>
          </el-form-item>
        </el-form>
      </el-card>
    </el-col>
  </el-row>
</template>

<style scoped>
.password-card {
  margin-top: 16px;
}

.role-tag {
  margin-right: 6px;
}

.hint {
  margin-left: 12px;
  font-size: 12px;
  color: var(--sf-text-secondary);
}
</style>
