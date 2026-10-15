<script setup>
import { onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'

import {
  assignUserRoles,
  createUser,
  fetchRoles,
  fetchUsers,
  resetUserPassword,
  updateUser,
  updateUserStatus
} from '@/api/system'
import { formatDateTime } from '@/utils/format'
import { normalizeError } from '@/utils/request'

const loading = ref(false)
const records = ref([])
const total = ref(0)
const roles = ref([])
const submitting = ref(false)
const dialogVisible = ref(false)
const editingId = ref(null)
const roleDialog = reactive({ visible: false, userId: null, username: '', roleIds: [] })
const passwordDialog = reactive({ visible: false, userId: null, username: '', newPassword: '' })

const query = reactive({ keyword: '', status: null, roleCode: '', pageNum: 1, pageSize: 10 })
const form = reactive({
  username: '',
  password: '',
  realName: '',
  phone: '',
  email: '',
  remark: '',
  roleIds: []
})

async function load() {
  loading.value = true
  try {
    const params = { ...query }
    Object.keys(params).forEach(key => {
      if (params[key] === null || params[key] === '') delete params[key]
    })
    const data = await fetchUsers(params)
    records.value = data.records || []
    total.value = data.total || 0
  } catch (error) {
    ElMessage.error(normalizeError(error).message)
  } finally {
    loading.value = false
  }
}

async function loadRoles() {
  try {
    roles.value = (await fetchRoles()) || []
  } catch (error) {
    ElMessage.error(normalizeError(error).message)
  }
}

function resetForm() {
  Object.assign(form, {
    username: '',
    password: '',
    realName: '',
    phone: '',
    email: '',
    remark: '',
    roleIds: []
  })
}

function openCreate() {
  editingId.value = null
  resetForm()
  dialogVisible.value = true
}

function openEdit(row) {
  editingId.value = row.id
  Object.assign(form, {
    username: row.username,
    password: '',
    realName: row.realName,
    phone: row.phone || '',
    email: row.email || '',
    remark: row.remark || '',
    roleIds: (row.roles || []).map(role => role.id)
  })
  dialogVisible.value = true
}

async function submit() {
  if (!form.realName.trim() || !form.roleIds.length) {
    ElMessage.warning('真实姓名与角色为必填项')
    return
  }
  submitting.value = true
  try {
    if (editingId.value) {
      await updateUser(editingId.value, {
        realName: form.realName,
        phone: form.phone,
        email: form.email,
        remark: form.remark
      })
      await assignUserRoles(editingId.value, form.roleIds)
      ElMessage.success('用户已更新')
    } else {
      if (!/^[a-zA-Z][a-zA-Z0-9_]{3,49}$/.test(form.username)) {
        ElMessage.warning('登录账号需以字母开头，由字母、数字、下划线组成，长度 4~50')
        return
      }
      if (form.password.length < 8) {
        ElMessage.warning('初始密码长度至少 8 位')
        return
      }
      await createUser({ ...form })
      ElMessage.success('用户已创建')
    }
    dialogVisible.value = false
    await load()
  } catch (error) {
    ElMessage.error(normalizeError(error).message)
  } finally {
    submitting.value = false
  }
}

function openRoleDialog(row) {
  roleDialog.visible = true
  roleDialog.userId = row.id
  roleDialog.username = row.username
  roleDialog.roleIds = (row.roles || []).map(role => role.id)
}

async function submitRoles() {
  if (!roleDialog.roleIds.length) {
    ElMessage.warning('至少选择一个角色')
    return
  }
  try {
    await assignUserRoles(roleDialog.userId, roleDialog.roleIds)
    ElMessage.success('角色已更新')
    roleDialog.visible = false
    await load()
  } catch (error) {
    ElMessage.error(normalizeError(error).message)
  }
}

async function toggleStatus(row) {
  const nextStatus = row.status === 1 ? 0 : 1
  try {
    await ElMessageBox.confirm(
      nextStatus === 0 ? `确认禁用账号「${row.username}」？` : `确认启用账号「${row.username}」？`,
      '提示',
      { type: 'warning' }
    )
    await updateUserStatus(row.id, nextStatus)
    ElMessage.success('状态已更新')
    await load()
  } catch (error) {
    if (error !== 'cancel') ElMessage.error(normalizeError(error).message)
  }
}

function openPasswordDialog(row) {
  passwordDialog.visible = true
  passwordDialog.userId = row.id
  passwordDialog.username = row.username
  passwordDialog.newPassword = ''
}

async function submitPassword() {
  if (passwordDialog.newPassword.length < 8) {
    ElMessage.warning('新密码长度至少 8 位')
    return
  }
  try {
    await resetUserPassword(passwordDialog.userId, passwordDialog.newPassword)
    ElMessage.success('密码已重置，并已解除登录锁定')
    passwordDialog.visible = false
  } catch (error) {
    ElMessage.error(normalizeError(error).message)
  }
}

onMounted(() => {
  load()
  loadRoles()
})
</script>

<template>
  <div class="page">
    <el-card shadow="never">
      <el-form :inline="true" @submit.prevent>
        <el-form-item label="关键词">
          <el-input v-model="query.keyword" placeholder="账号 / 姓名 / 手机号" clearable style="width: 200px"
                    @keyup.enter="load" />
        </el-form-item>
        <el-form-item label="角色">
          <el-select v-model="query.roleCode" placeholder="全部角色" clearable style="width: 160px">
            <el-option v-for="role in roles" :key="role.id" :label="role.roleName" :value="role.roleCode" />
          </el-select>
        </el-form-item>
        <el-form-item label="状态">
          <el-select v-model="query.status" placeholder="全部" clearable style="width: 110px">
            <el-option label="启用" :value="1" />
            <el-option label="禁用" :value="0" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="load">查询</el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <el-card shadow="never">
      <template #header>
        <div class="card-header">
          <span>用户列表（共 {{ total }} 个）</span>
          <el-button type="primary" @click="openCreate">
            <el-icon><Plus /></el-icon>新增用户
          </el-button>
        </div>
      </template>

      <el-table v-loading="loading" :data="records" stripe>
        <el-table-column prop="username" label="登录账号" width="140" />
        <el-table-column prop="realName" label="姓名" width="120" />
        <el-table-column prop="phone" label="手机号" width="140" />
        <el-table-column label="角色" min-width="180">
          <template #default="{ row }">
            <el-tag v-for="role in row.roles" :key="role.id" size="small" class="role-tag">
              {{ role.roleName }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="状态" width="90">
          <template #default="{ row }">
            <el-tag :type="row.status === 1 ? 'success' : 'danger'" size="small">
              {{ row.status === 1 ? '启用' : '禁用' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="最后登录" width="170">
          <template #default="{ row }">{{ formatDateTime(row.lastLoginTime) }}</template>
        </el-table-column>
        <el-table-column label="操作" width="260" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" @click="openEdit(row)">编辑</el-button>
            <el-button link type="primary" @click="openRoleDialog(row)">角色</el-button>
            <el-button link type="warning" @click="toggleStatus(row)">
              {{ row.status === 1 ? '禁用' : '启用' }}
            </el-button>
            <el-button link type="danger" @click="openPasswordDialog(row)">重置密码</el-button>
          </template>
        </el-table-column>
        <template #empty><el-empty description="没有符合条件的用户" /></template>
      </el-table>

      <el-pagination v-model:current-page="query.pageNum" v-model:page-size="query.pageSize" class="pagination"
                     background layout="total, sizes, prev, pager, next" :page-sizes="[10, 20, 50]" :total="total"
                     @current-change="load" @size-change="load" />
    </el-card>

    <el-dialog v-model="dialogVisible" :title="editingId ? '编辑用户' : '新增用户'" width="560px">
      <el-form :model="form" label-width="100px">
        <el-form-item label="登录账号" required>
          <el-input v-model="form.username" :disabled="Boolean(editingId)" />
        </el-form-item>
        <el-form-item v-if="!editingId" label="初始密码" required>
          <el-input v-model="form.password" type="password" show-password
                    placeholder="至少 8 位，需包含字母与数字" />
        </el-form-item>
        <el-form-item label="真实姓名" required><el-input v-model="form.realName" maxlength="50" /></el-form-item>
        <el-form-item label="手机号"><el-input v-model="form.phone" maxlength="20" /></el-form-item>
        <el-form-item label="邮箱"><el-input v-model="form.email" maxlength="100" /></el-form-item>
        <el-form-item label="角色" required>
          <el-select v-model="form.roleIds" multiple style="width: 100%">
            <el-option v-for="role in roles" :key="role.id" :label="role.roleName" :value="role.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="备注">
          <el-input v-model="form.remark" type="textarea" :rows="2" maxlength="255" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitting" @click="submit">保存</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="roleDialog.visible" :title="`分配角色 - ${roleDialog.username}`" width="460px">
      <el-select v-model="roleDialog.roleIds" multiple style="width: 100%">
        <el-option v-for="role in roles" :key="role.id" :label="role.roleName" :value="role.id" />
      </el-select>
      <template #footer>
        <el-button @click="roleDialog.visible = false">取消</el-button>
        <el-button type="primary" @click="submitRoles">保存</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="passwordDialog.visible" :title="`重置密码 - ${passwordDialog.username}`" width="460px">
      <el-input v-model="passwordDialog.newPassword" type="password" show-password
                placeholder="新密码，至少 8 位且包含字母与数字" />
      <template #footer>
        <el-button @click="passwordDialog.visible = false">取消</el-button>
        <el-button type="primary" @click="submitPassword">确认重置</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<style scoped>
.page {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.card-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.role-tag {
  margin-right: 6px;
}

.pagination {
  margin-top: 16px;
  justify-content: flex-end;
}
</style>
