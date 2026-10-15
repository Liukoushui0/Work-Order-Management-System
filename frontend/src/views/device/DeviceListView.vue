<script setup>
import { onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'

import {
  createDevice,
  deleteDevice,
  fetchCustomerOptions,
  fetchDevices,
  updateDevice
} from '@/api/customer'
import { useAuthStore } from '@/stores/auth'
import { formatDate, formatDateTime } from '@/utils/format'
import { normalizeError } from '@/utils/request'

const authStore = useAuthStore()
const loading = ref(false)
const records = ref([])
const total = ref(0)
const customers = ref([])
const dialogVisible = ref(false)
const submitting = ref(false)
const editingId = ref(null)

const query = reactive({ keyword: '', customerId: null, status: null, pageNum: 1, pageSize: 10 })
const form = reactive({
  deviceCode: '',
  deviceName: '',
  deviceType: '',
  brand: '',
  model: '',
  snNo: '',
  customerId: null,
  purchaseDate: '',
  warrantyExpireDate: '',
  installAddress: '',
  status: 1,
  remark: ''
})

const canManage = () => authStore.hasPermission('device:manage')

async function load() {
  loading.value = true
  try {
    const params = { ...query }
    Object.keys(params).forEach(key => {
      if (params[key] === null || params[key] === '') delete params[key]
    })
    const data = await fetchDevices(params)
    records.value = data.records || []
    total.value = data.total || 0
  } catch (error) {
    ElMessage.error(normalizeError(error).message)
  } finally {
    loading.value = false
  }
}

async function loadCustomers() {
  try {
    customers.value = (await fetchCustomerOptions()) || []
  } catch (error) {
    ElMessage.error(normalizeError(error).message)
  }
}

function resetForm() {
  Object.assign(form, {
    deviceCode: '',
    deviceName: '',
    deviceType: '',
    brand: '',
    model: '',
    snNo: '',
    customerId: null,
    purchaseDate: '',
    warrantyExpireDate: '',
    installAddress: '',
    status: 1,
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
    deviceCode: row.deviceCode,
    deviceName: row.deviceName,
    deviceType: row.deviceType || '',
    brand: row.brand || '',
    model: row.model || '',
    snNo: row.snNo || '',
    customerId: row.customerId,
    purchaseDate: row.purchaseDate || '',
    warrantyExpireDate: row.warrantyExpireDate || '',
    installAddress: row.installAddress || '',
    status: row.status,
    remark: row.remark || ''
  })
  dialogVisible.value = true
}

async function submit() {
  if (!form.deviceName.trim() || !form.customerId) {
    ElMessage.warning('设备名称与所属客户为必填项')
    return
  }
  submitting.value = true
  try {
    const payload = { ...form }
    if (!payload.purchaseDate) delete payload.purchaseDate
    if (!payload.warrantyExpireDate) delete payload.warrantyExpireDate
    if (editingId.value) {
      delete payload.deviceCode
      await updateDevice(editingId.value, payload)
      ElMessage.success('设备已更新')
    } else {
      if (!/^[A-Z0-9-]{2,32}$/.test(form.deviceCode)) {
        ElMessage.warning('设备编号只能包含大写字母、数字与短横线')
        return
      }
      await createDevice(payload)
      ElMessage.success('设备已创建')
    }
    dialogVisible.value = false
    await load()
  } catch (error) {
    ElMessage.error(normalizeError(error).message)
  } finally {
    submitting.value = false
  }
}

async function remove(row) {
  try {
    await ElMessageBox.confirm(`确认删除设备「${row.deviceName}」？`, '提示', { type: 'warning' })
    await deleteDevice(row.id)
    ElMessage.success('设备已删除')
    await load()
  } catch (error) {
    if (error !== 'cancel') ElMessage.error(normalizeError(error).message)
  }
}

onMounted(() => {
  load()
  loadCustomers()
})
</script>

<template>
  <div class="page">
    <el-card shadow="never">
      <el-form :inline="true" @submit.prevent>
        <el-form-item label="关键词">
          <el-input v-model="query.keyword" placeholder="设备编号 / 名称 / 型号 / 序列号" clearable
                    style="width: 240px" @keyup.enter="load" />
        </el-form-item>
        <el-form-item v-if="!authStore.isCustomer" label="所属客户">
          <el-select v-model="query.customerId" placeholder="全部客户" clearable filterable style="width: 200px">
            <el-option v-for="item in customers" :key="item.id" :label="item.customerName" :value="item.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="状态">
          <el-select v-model="query.status" placeholder="全部" clearable style="width: 120px">
            <el-option label="在用" :value="1" />
            <el-option label="停用" :value="0" />
            <el-option label="报废" :value="2" />
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
          <span>设备档案（共 {{ total }} 台）</span>
          <el-button v-if="canManage()" type="primary" @click="openCreate">
            <el-icon><Plus /></el-icon>新建设备
          </el-button>
        </div>
      </template>

      <el-table v-loading="loading" :data="records" stripe>
        <el-table-column prop="deviceCode" label="设备编号" width="130" />
        <el-table-column prop="deviceName" label="设备名称" min-width="170" show-overflow-tooltip />
        <el-table-column prop="customerName" label="所属客户" min-width="160" show-overflow-tooltip />
        <el-table-column prop="model" label="型号" width="130" />
        <el-table-column prop="snNo" label="序列号" width="140" />
        <el-table-column label="保修" width="150">
          <template #default="{ row }">
            {{ formatDate(row.warrantyExpireDate) }}
            <el-tag :type="row.inWarranty ? 'success' : 'warning'" size="small">
              {{ row.inWarranty ? '保内' : '保外' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="状态" width="90">
          <template #default="{ row }">
            <el-tag :type="row.status === 1 ? 'success' : 'info'" size="small">
              {{ row.status === 1 ? '在用' : row.status === 0 ? '停用' : '报废' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="ticketCount" label="工单数" width="90" />
        <el-table-column label="创建时间" width="170">
          <template #default="{ row }">{{ formatDateTime(row.createTime) }}</template>
        </el-table-column>
        <el-table-column v-if="canManage()" label="操作" width="130" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" @click="openEdit(row)">编辑</el-button>
            <el-button link type="danger" @click="remove(row)">删除</el-button>
          </template>
        </el-table-column>
        <template #empty><el-empty description="没有符合条件的设备" /></template>
      </el-table>

      <el-pagination v-model:current-page="query.pageNum" v-model:page-size="query.pageSize" class="pagination"
                     background layout="total, sizes, prev, pager, next" :page-sizes="[10, 20, 50]" :total="total"
                     @current-change="load" @size-change="load" />
    </el-card>

    <el-dialog v-model="dialogVisible" :title="editingId ? '编辑设备' : '新建设备'" width="680px">
      <el-form :model="form" label-width="110px">
        <el-row :gutter="16">
          <el-col :span="12">
            <el-form-item label="设备编号" required>
              <el-input v-model="form.deviceCode" :disabled="Boolean(editingId)" placeholder="如 DEV-0009" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="设备名称" required>
              <el-input v-model="form.deviceName" maxlength="100" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="所属客户" required>
              <el-select v-model="form.customerId" filterable style="width: 100%">
                <el-option v-for="item in customers" :key="item.id" :label="item.customerName" :value="item.id" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="设备类型"><el-input v-model="form.deviceType" maxlength="50" /></el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="品牌"><el-input v-model="form.brand" maxlength="50" /></el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="型号"><el-input v-model="form.model" maxlength="50" /></el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="序列号"><el-input v-model="form.snNo" maxlength="64" /></el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="状态">
              <el-select v-model="form.status" style="width: 100%">
                <el-option label="在用" :value="1" />
                <el-option label="停用" :value="0" />
                <el-option label="报废" :value="2" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="购买日期">
              <el-date-picker v-model="form.purchaseDate" type="date" value-format="YYYY-MM-DD" style="width: 100%" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="保修截止">
              <el-date-picker v-model="form.warrantyExpireDate" type="date" value-format="YYYY-MM-DD"
                              style="width: 100%" />
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="安装地址"><el-input v-model="form.installAddress" maxlength="255" /></el-form-item>
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
