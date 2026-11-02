<script setup>
import { onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'

import {
  createCustomer,
  deleteCustomer,
  fetchCustomers,
  updateCustomer,
  updateCustomerStatus
} from '@/api/customer'
import { useAuthStore } from '@/stores/auth'
import { CUSTOMER_LEVEL_OPTIONS, formatDateTime } from '@/utils/format'
import { normalizeError } from '@/utils/request'

const authStore = useAuthStore()
const loading = ref(false)
const records = ref([])
const total = ref(0)
const dialogVisible = ref(false)
const submitting = ref(false)
const editingId = ref(null)
const query = reactive({ keyword: '', level: '', status: null, pageNum: 1, pageSize: 10 })
const form = reactive({
  customerCode: '',
  customerName: '',
  contactName: '',
  contactPhone: '',
  contactEmail: '',
  province: '',
  city: '',
  address: '',
  industry: '',
  level: 'B',
  source: 'WEB',
  remark: ''
})

const canManage = () => authStore.hasPermission('customer:manage')

async function load() {
  loading.value = true
  try {
    const params = { ...query }
    if (params.status === null) delete params.status
    if (!params.level) delete params.level
    const data = await fetchCustomers(params)
    records.value = data.records || []
    total.value = data.total || 0
  } catch (error) {
    ElMessage.error(normalizeError(error).message)
  } finally {
    loading.value = false
  }
}

function resetForm() {
  Object.assign(form, {
    customerCode: '',
    customerName: '',
    contactName: '',
    contactPhone: '',
    contactEmail: '',
    province: '',
    city: '',
    address: '',
    industry: '',
    level: 'B',
    source: 'WEB',
    remark: ''
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
    customerCode: row.customerCode,
    customerName: row.customerName,
    contactName: row.contactName,
    contactPhone: row.contactPhone,
    contactEmail: row.contactEmail || '',
    province: row.province || '',
    city: row.city || '',
    address: row.address || '',
    industry: row.industry || '',
    level: row.level || 'B',
    source: row.source || 'WEB',
    remark: row.remark || ''
  })
  dialogVisible.value = true
}

async function submit() {
  if (!form.customerName.trim() || !form.contactName.trim() || !form.contactPhone.trim()) {
    ElMessage.warning('客户名称、联系人、联系电话为必填项')
    return
  }
  if (!/^1[3-9]\d{9}$/.test(form.contactPhone)) {
    ElMessage.warning('联系电话格式不正确')
    return
  }
  submitting.value = true
  try {
    if (editingId.value) {
      const payload = { ...form }
      delete payload.customerCode
      await updateCustomer(editingId.value, payload)
      ElMessage.success('客户已更新')
    } else {
      if (!/^[A-Z0-9-]{2,32}$/.test(form.customerCode)) {
        ElMessage.warning('客户编号只能包含大写字母、数字与短横线')
        return
      }
      await createCustomer({ ...form })
      ElMessage.success('客户已创建')
    }
    dialogVisible.value = false
    await load()
  } catch (error) {
    ElMessage.error(normalizeError(error).message)
  } finally {
    submitting.value = false
  }
}

async function toggleStatus(row) {
  const nextStatus = row.status === 1 ? 0 : 1
  try {
    await ElMessageBox.confirm(
      nextStatus === 0 ? `确认停用客户「${row.customerName}」？` : `确认启用客户「${row.customerName}」？`,
      '提示',
      { type: 'warning' }
    )
    await updateCustomerStatus(row.id, nextStatus)
    ElMessage.success('状态已更新')
    await load()
  } catch (error) {
    if (error !== 'cancel') ElMessage.error(normalizeError(error).message)
  }
}

async function remove(row) {
  try {
    await ElMessageBox.confirm(`确认删除客户「${row.customerName}」？`, '提示', { type: 'warning' })
    await deleteCustomer(row.id)
    ElMessage.success('客户已删除')
    await load()
  } catch (error) {
    if (error !== 'cancel') ElMessage.error(normalizeError(error).message)
  }
}

onMounted(load)
</script>

<template>
  <div class="page">
    <el-card shadow="never">
      <el-form :inline="true" @submit.prevent>
        <el-form-item label="关键词">
          <el-input v-model="query.keyword" placeholder="编号 / 名称 / 联系人 / 电话" clearable
                    style="width: 220px" @keyup.enter="load" />
        </el-form-item>
        <el-form-item label="等级">
          <el-select v-model="query.level" placeholder="全部" clearable style="width: 110px">
            <el-option v-for="item in CUSTOMER_LEVEL_OPTIONS" :key="item.value" :label="item.label"
                       :value="item.value" />
          </el-select>
        </el-form-item>
        <el-form-item label="状态">
          <el-select v-model="query.status" placeholder="全部" clearable style="width: 110px">
            <el-option label="正常" :value="1" />
            <el-option label="停用" :value="0" />
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
          <span>客户列表（共 {{ total }} 条）</span>
          <el-button v-if="canManage()" type="primary" @click="openCreate">
            <el-icon><Plus /></el-icon>新增客户
          </el-button>
        </div>
      </template>

      <el-table v-loading="loading" :data="records" stripe>
        <el-table-column prop="customerCode" label="客户编号" width="120" />
        <el-table-column prop="customerName" label="客户名称" min-width="150" show-overflow-tooltip />
        <el-table-column prop="contactName" label="联系人" width="100" />
        <el-table-column prop="contactPhone" label="联系电话" width="125" />
        <el-table-column prop="level" label="等级" width="65" />
        <el-table-column prop="deviceCount" label="设备数" width="80" />
        <el-table-column prop="ticketCount" label="工单数" width="80" />
        <el-table-column label="状态" width="80">
          <template #default="{ row }">
            <el-tag :type="row.status === 1 ? 'success' : 'info'" size="small">
              {{ row.status === 1 ? '正常' : '停用' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="创建时间" width="160">
          <template #default="{ row }">{{ formatDateTime(row.createTime) }}</template>
        </el-table-column>
        <el-table-column v-if="canManage()" label="操作" width="170" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" @click="openEdit(row)">编辑</el-button>
            <el-button link type="warning" @click="toggleStatus(row)">
              {{ row.status === 1 ? '停用' : '启用' }}
            </el-button>
            <el-button link type="danger" @click="remove(row)">删除</el-button>
          </template>
        </el-table-column>
        <template #empty><el-empty description="没有符合条件的客户" /></template>
      </el-table>

      <el-pagination v-model:current-page="query.pageNum" v-model:page-size="query.pageSize" class="pagination"
                     background layout="total, sizes, prev, pager, next" :page-sizes="[10, 20, 50]" :total="total"
                     @current-change="load" @size-change="load" />
    </el-card>

    <el-dialog v-model="dialogVisible" :title="editingId ? '编辑客户' : '新增客户'" width="680px">
      <el-form :model="form" label-width="100px">
        <el-row :gutter="16">
          <el-col :span="12">
            <el-form-item label="客户编号" required>
              <el-input v-model="form.customerCode" :disabled="Boolean(editingId)" placeholder="如 CUS-0005" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="客户名称" required>
              <el-input v-model="form.customerName" maxlength="100" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="联系人" required>
              <el-input v-model="form.contactName" maxlength="50" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="联系电话" required>
              <el-input v-model="form.contactPhone" maxlength="20" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="联系邮箱">
              <el-input v-model="form.contactEmail" maxlength="100" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="客户等级">
              <el-select v-model="form.level" style="width: 100%">
                <el-option v-for="item in CUSTOMER_LEVEL_OPTIONS" :key="item.value" :label="item.label"
                           :value="item.value" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="省份"><el-input v-model="form.province" maxlength="50" /></el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="城市"><el-input v-model="form.city" maxlength="50" /></el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="详细地址"><el-input v-model="form.address" maxlength="255" /></el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="备注">
              <el-input v-model="form.remark" type="textarea" :rows="2" maxlength="255" />
            </el-form-item>
          </el-col>
        </el-row>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitting" @click="submit">保存</el-button>
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

.pagination {
  margin-top: 16px;
  justify-content: flex-end;
}
</style>
