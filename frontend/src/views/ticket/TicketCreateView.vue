<script setup>
import { onMounted, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'

import { fetchCustomerOptions, fetchDeviceOptions } from '@/api/customer'
import { createTicket } from '@/api/ticket'
import { fetchEnabledFaultCategories } from '@/api/system'
import { TICKET_PRIORITY_OPTIONS, TICKET_SOURCE_OPTIONS } from '@/utils/format'
import { normalizeError } from '@/utils/request'

const router = useRouter()
const formRef = ref(null)
const submitting = ref(false)
const customers = ref([])
const devices = ref([])
const categories = ref([])
const loadingCustomer = ref(false)
const loadingDevice = ref(false)

const form = reactive({
  customerId: null,
  deviceId: null,
  faultCategoryId: null,
  title: '',
  description: '',
  priority: 'MEDIUM',
  source: 'WEB',
  reporterName: '',
  reporterPhone: '',
  deadlineTime: null
})

const rules = {
  customerId: [{ required: true, message: '请选择客户', trigger: 'change' }],
  deviceId: [{ required: true, message: '请选择设备', trigger: 'change' }],
  title: [
    { required: true, message: '请输入工单标题', trigger: 'blur' },
    { max: 200, message: '标题长度不能超过 200', trigger: 'blur' }
  ],
  description: [
    { required: true, message: '请填写故障描述', trigger: 'blur' },
    { max: 2000, message: '故障描述长度不能超过 2000', trigger: 'blur' }
  ],
  priority: [{ required: true, message: '请选择优先级', trigger: 'change' }],
  reporterPhone: [
    { pattern: /^$|^1[3-9]\d{9}$/, message: '手机号格式不正确', trigger: 'blur' }
  ]
}

async function loadCustomers() {
  loadingCustomer.value = true
  try {
    customers.value = (await fetchCustomerOptions()) || []
  } catch (error) {
    ElMessage.error(normalizeError(error).message)
  } finally {
    loadingCustomer.value = false
  }
}

async function loadCategories() {
  try {
    categories.value = (await fetchEnabledFaultCategories()) || []
  } catch (error) {
    ElMessage.error(normalizeError(error).message)
  }
}

async function handleCustomerChange(customerId) {
  form.deviceId = null
  devices.value = []
  if (!customerId) return
  loadingDevice.value = true
  try {
    devices.value = (await fetchDeviceOptions(customerId)) || []
    const customer = customers.value.find(item => item.id === customerId)
    if (customer && !form.reporterName) {
      form.reporterName = customer.contactName || ''
    }
    if (customer && !form.reporterPhone) {
      form.reporterPhone = customer.contactPhone || ''
    }
  } catch (error) {
    ElMessage.error(normalizeError(error).message)
  } finally {
    loadingDevice.value = false
  }
}

async function submit() {
  const valid = await formRef.value.validate().catch(() => false)
  if (!valid) return
  submitting.value = true
  try {
    const created = await createTicket({ ...form })
    ElMessage.success(`工单创建成功：${created.ticketNo}`)
    router.push(`/tickets/${created.id}`)
  } catch (error) {
    ElMessage.error(normalizeError(error).message)
  } finally {
    submitting.value = false
  }
}

onMounted(() => {
  loadCustomers()
  loadCategories()
})
</script>

<template>
  <el-card shadow="never">
    <template #header>
      <div class="card-header">
        <span>创建工单</span>
        <el-button link @click="router.back()">返回</el-button>
      </div>
    </template>

    <el-form ref="formRef" :model="form" :rules="rules" label-width="110px" class="create-form">
      <el-row :gutter="16">
        <el-col :xs="24" :md="12">
          <el-form-item label="客户" prop="customerId">
            <el-select v-model="form.customerId" filterable placeholder="请选择客户" :loading="loadingCustomer"
                       style="width: 100%" @change="handleCustomerChange">
              <el-option v-for="item in customers" :key="item.id"
                         :label="`${item.customerName}（${item.customerCode}）`" :value="item.id" />
            </el-select>
          </el-form-item>
        </el-col>
        <el-col :xs="24" :md="12">
          <el-form-item label="设备" prop="deviceId">
            <el-select v-model="form.deviceId" filterable placeholder="请先选择客户" :loading="loadingDevice"
                       :disabled="!form.customerId" style="width: 100%">
              <el-option v-for="item in devices" :key="item.id"
                         :label="`${item.deviceName}（${item.deviceCode}）`" :value="item.id" />
            </el-select>
            <div v-if="form.customerId && !devices.length" class="form-hint">该客户暂无可用设备</div>
          </el-form-item>
        </el-col>
      </el-row>

      <el-row :gutter="16">
        <el-col :xs="24" :md="12">
          <el-form-item label="故障分类" prop="faultCategoryId">
            <el-select v-model="form.faultCategoryId" placeholder="请选择故障分类" clearable style="width: 100%">
              <el-option v-for="item in categories" :key="item.id" :label="item.categoryName" :value="item.id" />
            </el-select>
          </el-form-item>
        </el-col>
        <el-col :xs="24" :md="12">
          <el-form-item label="优先级" prop="priority">
            <el-select v-model="form.priority" style="width: 100%">
              <el-option v-for="item in TICKET_PRIORITY_OPTIONS" :key="item.value" :label="item.label"
                         :value="item.value" />
            </el-select>
          </el-form-item>
        </el-col>
      </el-row>

      <el-form-item label="工单标题" prop="title">
        <el-input v-model="form.title" maxlength="200" show-word-limit placeholder="简要描述故障现象" />
      </el-form-item>

      <el-form-item label="故障描述" prop="description">
        <el-input v-model="form.description" type="textarea" :rows="4" maxlength="2000" show-word-limit
                  placeholder="请描述故障现象、发生时间、影响范围等信息" />
      </el-form-item>

      <el-row :gutter="16">
        <el-col :xs="24" :md="8">
          <el-form-item label="来源" prop="source">
            <el-select v-model="form.source" style="width: 100%">
              <el-option v-for="item in TICKET_SOURCE_OPTIONS" :key="item.value" :label="item.label"
                         :value="item.value" />
            </el-select>
          </el-form-item>
        </el-col>
        <el-col :xs="24" :md="8">
          <el-form-item label="报修人" prop="reporterName">
            <el-input v-model="form.reporterName" placeholder="默认取客户联系人" maxlength="50" />
          </el-form-item>
        </el-col>
        <el-col :xs="24" :md="8">
          <el-form-item label="报修电话" prop="reporterPhone">
            <el-input v-model="form.reporterPhone" placeholder="默认取客户联系电话" maxlength="20" />
          </el-form-item>
        </el-col>
      </el-row>

      <el-form-item label="SLA 截止">
        <el-date-picker v-model="form.deadlineTime" type="datetime" placeholder="留空按优先级默认 SLA 计算"
                        value-format="YYYY-MM-DDTHH:mm:ss" style="width: 260px" />
        <div class="form-hint">优先级对应默认 SLA：紧急 4h / 高 8h / 中 24h / 低 48h</div>
      </el-form-item>

      <el-form-item>
        <el-button type="primary" :loading="submitting" @click="submit">提交建单</el-button>
        <el-button @click="router.push('/tickets')">取消</el-button>
      </el-form-item>
    </el-form>
  </el-card>
</template>

<style scoped>
.card-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.create-form {
  max-width: 900px;
}

.form-hint {
  font-size: 12px;
  color: var(--sf-text-secondary);
  line-height: 1.6;
}
</style>
