<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'

import { fetchEngineers } from '@/api/system'
import {
  acceptTicket,
  assignTicket,
  cancelTicket,
  completeTicket,
  confirmTicket,
  createComment,
  createEvaluation,
  deleteAttachment,
  fetchAttachments,
  fetchComments,
  fetchEvaluation,
  fetchTicketDetail,
  processTicket,
  rejectTicket,
  reopenTicket,
  transferTicket,
  uploadAttachment
} from '@/api/ticket'
import { download, normalizeError } from '@/utils/request'
import StatusTag from '@/components/StatusTag.vue'
import { useAuthStore } from '@/stores/auth'
import {
  ATTACHMENT_BIZ_TYPES,
  formatDateTime,
  formatFileSize,
  satisfactionLabel
} from '@/utils/format'

const route = useRoute()
const router = useRouter()
const authStore = useAuthStore()
const ticketId = route.params.id

const loading = ref(false)
const detail = ref(null)
const comments = ref([])
const attachments = ref([])
const evaluation = ref(null)
const engineers = ref([])
const submitting = ref(false)
const uploadFileList = ref([])
const uploadBizType = ref('FAULT_PHOTO')
const commentForm = reactive({ content: '', internal: false })

const actionDialog = reactive({
  visible: false,
  type: '',
  title: '',
  form: {
    assigneeId: null,
    reason: '',
    remark: '',
    content: '',
    usedHours: null,
    result: '',
    score: 5,
    evaluationContent: '',
    anonymous: false,
    tags: ''
  }
})

const ticket = computed(() => detail.value?.ticket || null)
const isAssignee = computed(() => ticket.value && ticket.value.assigneeId === authStore.user?.id)
const canEvaluate = computed(
  () =>
    ticket.value &&
    ticket.value.status === 'COMPLETED' &&
    !evaluation.value &&
    authStore.isCustomer
)

const canAssign = computed(
  () => ticket.value && ticket.value.status === 'PENDING_ASSIGN' && authStore.hasPermission('ticket:assign')
)
const canAccept = computed(
  () => ticket.value && ticket.value.status === 'PENDING_ACCEPT' && isAssignee.value
)
const canTransfer = computed(
  () =>
    ticket.value &&
    ['PENDING_ACCEPT', 'PROCESSING'].includes(ticket.value.status) &&
    authStore.hasPermission('ticket:transfer') &&
    (isAssignee.value || authStore.isAdmin || authStore.isService)
)
const canReject = computed(
  () => ticket.value && ticket.value.status === 'PENDING_ACCEPT' && isAssignee.value
)
const canProcess = computed(
  () => ticket.value && ticket.value.status === 'PROCESSING' && isAssignee.value
)
const canComplete = computed(
  () => ticket.value && ticket.value.status === 'PROCESSING' && isAssignee.value
)
const canConfirm = computed(
  () => ticket.value && ticket.value.status === 'PENDING_CONFIRM' && authStore.isCustomer
)
const canReopen = computed(
  () =>
    ticket.value &&
    ['PENDING_CONFIRM', 'COMPLETED'].includes(ticket.value.status) &&
    authStore.hasPermission('ticket:reopen') &&
    (authStore.isCustomer || authStore.isAdmin || authStore.isService)
)
const canCancel = computed(
  () =>
    ticket.value &&
    !['COMPLETED', 'CANCELLED'].includes(ticket.value.status) &&
    authStore.hasPermission('ticket:cancel')
)

async function load() {
  loading.value = true
  try {
    const [detailData, commentData, attachmentData, evaluationData] = await Promise.all([
      fetchTicketDetail(ticketId),
      fetchComments(ticketId),
      fetchAttachments(ticketId),
      fetchEvaluation(ticketId)
    ])
    detail.value = detailData
    comments.value = commentData || []
    attachments.value = attachmentData || []
    evaluation.value = evaluationData || null
  } catch (error) {
    ElMessage.error(normalizeError(error).message)
  } finally {
    loading.value = false
  }
}

async function loadEngineers() {
  try {
    engineers.value = (await fetchEngineers()) || []
  } catch (error) {
    engineers.value = []
    ElMessage.error(normalizeError(error).message)
  }
}

function openAction(type) {
  const titles = {
    assign: '分派工单',
    transfer: '转派工单',
    reject: '退回工单',
    process: '填写处理记录',
    complete: '提交处理结果',
    confirm: '确认服务结果',
    reopen: '退回重做或重新打开',
    cancel: '取消工单'
  }
  actionDialog.type = type
  actionDialog.title = titles[type] || '工单操作'
  actionDialog.form.assigneeId = null
  actionDialog.form.reason = ''
  actionDialog.form.remark = ''
  actionDialog.form.content = ''
  actionDialog.form.usedHours = null
  actionDialog.form.result = ''
  actionDialog.visible = true
  if (type === 'assign' || type === 'transfer') {
    loadEngineers()
  }
}

async function submitAction() {
  const type = actionDialog.type
  const form = actionDialog.form
  if ((type === 'assign' || type === 'transfer') && !form.assigneeId) {
    ElMessage.warning('请选择目标工程师')
    return
  }
  if (['reject', 'transfer', 'reopen', 'cancel'].includes(type) && !form.reason.trim()) {
    ElMessage.warning('请填写原因')
    return
  }
  if (type === 'process' && !form.content.trim()) {
    ElMessage.warning('请填写处理说明')
    return
  }
  if (type === 'complete' && !form.result.trim()) {
    ElMessage.warning('请填写处理结果')
    return
  }
  submitting.value = true
  try {
    if (type === 'assign') {
      await assignTicket(ticketId, { assigneeId: form.assigneeId, remark: form.remark })
    } else if (type === 'transfer') {
      await transferTicket(ticketId, { assigneeId: form.assigneeId, reason: form.reason })
    } else if (type === 'reject') {
      await rejectTicket(ticketId, { reason: form.reason })
    } else if (type === 'process') {
      await processTicket(ticketId, { content: form.content, usedHours: form.usedHours })
    } else if (type === 'complete') {
      await completeTicket(ticketId, { result: form.result, usedHours: form.usedHours })
    } else if (type === 'confirm') {
      await confirmTicket(ticketId, { remark: form.remark })
    } else if (type === 'reopen') {
      await reopenTicket(ticketId, { reason: form.reason })
    } else if (type === 'cancel') {
      await cancelTicket(ticketId, { reason: form.reason })
    }
    ElMessage.success('操作成功')
    actionDialog.visible = false
    await load()
  } catch (error) {
    ElMessage.error(normalizeError(error).message)
  } finally {
    submitting.value = false
  }
}

async function doAccept() {
  try {
    await ElMessageBox.confirm('确认接单并开始处理该工单？', '接单确认', { type: 'warning' })
    await acceptTicket(ticketId)
    ElMessage.success('接单成功')
    await load()
  } catch (error) {
    if (error !== 'cancel') ElMessage.error(normalizeError(error).message)
  }
}

async function addComment() {
  const content = commentForm.content.trim()
  if (!content) {
    ElMessage.warning('请输入评论内容')
    return
  }
  try {
    await createComment(ticketId, { content, internal: commentForm.internal })
    commentForm.content = ''
    ElMessage.success('已提交')
    comments.value = (await fetchComments(ticketId)) || []
  } catch (error) {
    ElMessage.error(normalizeError(error).message)
  }
}

async function handleUpload() {
  const file = uploadFileList.value[0]?.raw
  if (!file) {
    ElMessage.warning('请选择文件')
    return
  }
  try {
    await uploadAttachment(ticketId, file, uploadBizType.value)
    ElMessage.success('上传成功')
    uploadFileList.value = []
    attachments.value = (await fetchAttachments(ticketId)) || []
  } catch (error) {
    ElMessage.error(normalizeError(error).message)
  }
}

async function handleDownload(record) {
  try {
    const { blob, fileName } = await download(
      `/tickets/${ticketId}/attachments/${record.id}/download`
    )
    const url = window.URL.createObjectURL(blob)
    const link = document.createElement('a')
    link.href = url
    link.download = fileName
    link.click()
    window.URL.revokeObjectURL(url)
  } catch (error) {
    ElMessage.error(normalizeError(error).message)
  }
}

async function handleDeleteAttachment(record) {
  try {
    await ElMessageBox.confirm(`确认删除附件「${record.fileName}」？`, '提示', { type: 'warning' })
    await deleteAttachment(ticketId, record.id)
    ElMessage.success('已删除')
    attachments.value = (await fetchAttachments(ticketId)) || []
  } catch (error) {
    if (error !== 'cancel') ElMessage.error(normalizeError(error).message)
  }
}

async function submitEvaluation() {
  try {
    await createEvaluation(ticketId, {
      score: actionDialog.form.score,
      content: actionDialog.form.evaluationContent,
      tags: actionDialog.form.tags,
      anonymous: actionDialog.form.anonymous
    })
    ElMessage.success('感谢您的评价')
    actionDialog.visible = false
    evaluation.value = await fetchEvaluation(ticketId)
    await load()
  } catch (error) {
    ElMessage.error(normalizeError(error).message)
  }
}

onMounted(load)
</script>

<template>
  <div v-loading="loading" class="page">
    <el-card v-if="ticket" shadow="never">
      <template #header>
        <div class="card-header">
          <div class="card-header__title">
            <span class="ticket-no">{{ ticket.ticketNo }}</span>
            <span>{{ ticket.title }}</span>
            <StatusTag :value="ticket.status" />
            <StatusTag kind="priority" :value="ticket.priority" />
            <el-tag v-if="ticket.overdue" type="danger" effect="dark" size="small">逾期</el-tag>
          </div>
          <div class="card-header__actions">
            <el-button v-if="canAssign" type="primary" @click="openAction('assign')">分派</el-button>
            <el-button v-if="canAccept" type="primary" @click="doAccept">接单</el-button>
            <el-button v-if="canProcess" type="primary" @click="openAction('process')">填写处理记录</el-button>
            <el-button v-if="canComplete" type="success" @click="openAction('complete')">提交处理结果</el-button>
            <el-button v-if="canConfirm" type="success" @click="openAction('confirm')">确认服务结果</el-button>
            <el-button v-if="canEvaluate" type="warning" @click="openAction('evaluate')">去评价</el-button>
            <el-button v-if="canReopen" @click="openAction('reopen')">退回重做 / 重新打开</el-button>
            <el-button v-if="canTransfer" @click="openAction('transfer')">转派</el-button>
            <el-button v-if="canReject" @click="openAction('reject')">退回</el-button>
            <el-button v-if="canCancel" type="danger" plain @click="openAction('cancel')">取消工单</el-button>
            <el-button @click="router.push('/tickets')">返回列表</el-button>
          </div>
        </div>
      </template>

      <el-descriptions :column="3" border>
        <el-descriptions-item label="客户">{{ ticket.customerName }}</el-descriptions-item>
        <el-descriptions-item label="联系人">
          {{ detail.customerContactName }}<span v-if="detail.customerContactPhone">（{{ detail.customerContactPhone }}）</span>
        </el-descriptions-item>
        <el-descriptions-item label="负责人">{{ ticket.assigneeName || '未分派' }}</el-descriptions-item>
        <el-descriptions-item label="设备">{{ ticket.deviceName }}（{{ ticket.deviceCode }}）</el-descriptions-item>
        <el-descriptions-item label="品牌型号">
          {{ detail.deviceBrand || '-' }} / {{ detail.deviceModel || '-' }}
          <el-tag v-if="detail.deviceInWarranty" type="success" size="small">保内</el-tag>
          <el-tag v-else type="warning" size="small">保外</el-tag>
        </el-descriptions-item>
        <el-descriptions-item label="故障分类">{{ ticket.faultCategoryName || '未分类' }}</el-descriptions-item>
        <el-descriptions-item label="报修人">
          {{ detail.reporterName || '-' }}<span v-if="detail.reporterPhone">（{{ detail.reporterPhone }}）</span>
        </el-descriptions-item>
        <el-descriptions-item label="来源">{{ ticket.source }}</el-descriptions-item>
        <el-descriptions-item label="截止时间">{{ formatDateTime(ticket.deadlineTime) }}</el-descriptions-item>
        <el-descriptions-item label="创建时间">{{ formatDateTime(ticket.createTime) }}</el-descriptions-item>
        <el-descriptions-item label="接单时间">{{ formatDateTime(ticket.acceptTime) }}</el-descriptions-item>
        <el-descriptions-item label="处理时长">
          {{ ticket.handleHours === null || ticket.handleHours === undefined ? '-' : `${ticket.handleHours} 小时` }}
        </el-descriptions-item>
        <el-descriptions-item label="故障描述" :span="3">
          <pre class="description-text">{{ detail.description }}</pre>
        </el-descriptions-item>
        <el-descriptions-item v-if="detail.cancelReason" label="取消原因" :span="3">
          {{ detail.cancelReason }}
        </el-descriptions-item>
      </el-descriptions>
    </el-card>

    <el-card v-if="evaluation" shadow="never">
      <template #header><span>服务评价</span></template>
      <el-descriptions :column="2" border>
        <el-descriptions-item label="评分">
          <el-rate :model-value="evaluation.score" disabled />
        </el-descriptions-item>
        <el-descriptions-item label="满意度">{{ satisfactionLabel(evaluation.satisfaction) }}</el-descriptions-item>
        <el-descriptions-item label="评价内容" :span="2">
          {{ evaluation.content || (evaluation.anonymous ? '匿名评价，内容仅评价人可见' : '-') }}
        </el-descriptions-item>
        <el-descriptions-item label="评价时间">{{ formatDateTime(evaluation.createTime) }}</el-descriptions-item>
        <el-descriptions-item label="标签">{{ evaluation.tags || '-' }}</el-descriptions-item>
      </el-descriptions>
    </el-card>

    <el-card shadow="never">
      <el-tabs>
        <el-tab-pane label="处理历史">
          <el-timeline v-if="detail?.records?.length">
            <el-timeline-item v-for="record in detail.records" :key="record.id"
                              :timestamp="formatDateTime(record.createTime)" placement="top">
              <div class="record">
                <div class="record__title">
                  <el-tag size="small" effect="plain">{{ record.actionLabel }}</el-tag>
                  <span class="record__operator">{{ record.operatorName }}</span>
                  <span v-if="record.usedHours" class="record__hours">工时 {{ record.usedHours }}h</span>
                </div>
                <div class="record__content">{{ record.content }}</div>
              </div>
            </el-timeline-item>
          </el-timeline>
          <el-empty v-else description="暂无处理记录" />
        </el-tab-pane>

        <el-tab-pane :label="`沟通记录（${comments.length}）`">
          <div class="comment-list">
            <div v-for="comment in comments" :key="comment.id" class="comment">
              <div class="comment__meta">
                <strong>{{ comment.operatorName }}</strong>
                <el-tag size="small" :type="comment.userType === 'CUSTOMER' ? 'success' : 'info'">
                  {{ comment.userType === 'CUSTOMER' ? '客户' : '员工' }}
                </el-tag>
                <el-tag v-if="comment.internal" size="small" type="warning">内部备注</el-tag>
                <span class="comment__time">{{ formatDateTime(comment.createTime) }}</span>
              </div>
              <div class="comment__content">{{ comment.content }}</div>
            </div>
            <el-empty v-if="!comments.length" description="暂无沟通记录" />
          </div>
          <div class="comment-form">
            <el-input v-model="commentForm.content" type="textarea" :rows="3" maxlength="1000" show-word-limit
                      placeholder="补充沟通信息或内部备注" />
            <div class="comment-form__actions">
              <el-checkbox v-if="!authStore.isCustomer" v-model="commentForm.internal">仅员工可见（内部备注）</el-checkbox>
              <el-button type="primary" @click="addComment">提交评论</el-button>
            </div>
          </div>
        </el-tab-pane>

        <el-tab-pane :label="`附件（${attachments.length}）`">
          <el-table :data="attachments" stripe>
            <el-table-column prop="fileName" label="文件名" min-width="200" show-overflow-tooltip />
            <el-table-column prop="fileExt" label="类型" width="90" />
            <el-table-column label="大小" width="110">
              <template #default="{ row }">{{ formatFileSize(row.fileSize) }}</template>
            </el-table-column>
            <el-table-column prop="bizType" label="业务类型" width="130" />
            <el-table-column label="上传时间" width="180">
              <template #default="{ row }">{{ formatDateTime(row.createTime) }}</template>
            </el-table-column>
            <el-table-column label="操作" width="150" fixed="right">
              <template #default="{ row }">
                <el-button link type="primary" @click="handleDownload(row)">下载</el-button>
                <el-button v-if="authStore.hasPermission('attachment:delete')" link type="danger"
                           @click="handleDeleteAttachment(row)">删除</el-button>
              </template>
            </el-table-column>
            <template #empty><el-empty description="暂无附件" /></template>
          </el-table>

          <div v-if="authStore.hasPermission('attachment:upload')" class="upload-area">
            <el-upload v-model:file-list="uploadFileList" :auto-upload="false" :limit="1">
              <el-button>选择文件</el-button>
            </el-upload>
            <el-select v-model="uploadBizType" style="width: 150px">
              <el-option v-for="item in ATTACHMENT_BIZ_TYPES" :key="item.value" :label="item.label"
                         :value="item.value" />
            </el-select>
            <el-button type="primary" @click="handleUpload">上传附件</el-button>
            <span class="upload-hint">支持 jpg/jpeg/png/gif/pdf/txt/log/zip/docx/xlsx，单文件不超过 20MB</span>
          </div>
        </el-tab-pane>
      </el-tabs>
    </el-card>

    <el-dialog v-model="actionDialog.visible" :title="actionDialog.title" width="520px">
      <el-form label-width="100px">
        <el-form-item v-if="actionDialog.type === 'assign' || actionDialog.type === 'transfer'" label="目标工程师">
          <el-select v-model="actionDialog.form.assigneeId" filterable placeholder="请选择工程师" style="width: 100%">
            <el-option v-for="item in engineers" :key="item.id" :label="item.realName" :value="item.id" />
          </el-select>
        </el-form-item>
        <el-form-item v-if="actionDialog.type === 'assign'" label="分派说明">
          <el-input v-model="actionDialog.form.remark" placeholder="可选" maxlength="255" />
        </el-form-item>
        <el-form-item v-if="['transfer', 'reject', 'reopen', 'cancel'].includes(actionDialog.type)" label="原因">
          <el-input v-model="actionDialog.form.reason" type="textarea" :rows="3" maxlength="500" show-word-limit
                    placeholder="请说明原因" />
        </el-form-item>
        <el-form-item v-if="actionDialog.type === 'process'" label="处理说明">
          <el-input v-model="actionDialog.form.content" type="textarea" :rows="4" maxlength="2000" show-word-limit
                    placeholder="记录本次维修处理的过程与结论" />
        </el-form-item>
        <el-form-item v-if="['process', 'complete'].includes(actionDialog.type)" label="本次工时">
          <el-input-number v-model="actionDialog.form.usedHours" :min="0" :max="999" :precision="1" />
          <span class="upload-hint">小时</span>
        </el-form-item>
        <el-form-item v-if="actionDialog.type === 'complete'" label="处理结果">
          <el-input v-model="actionDialog.form.result" type="textarea" :rows="4" maxlength="2000" show-word-limit
                    placeholder="说明最终处理结果，供客户确认" />
        </el-form-item>
        <el-form-item v-if="actionDialog.type === 'confirm'" label="确认备注">
          <el-input v-model="actionDialog.form.remark" placeholder="可选" maxlength="500" />
        </el-form-item>
        <template v-if="actionDialog.type === 'evaluate'">
          <el-form-item label="综合评分">
            <el-rate v-model="actionDialog.form.score" />
          </el-form-item>
          <el-form-item label="评价内容">
            <el-input v-model="actionDialog.form.evaluationContent" type="textarea" :rows="3" maxlength="1000"
                      show-word-limit placeholder="请描述本次服务体验" />
          </el-form-item>
          <el-form-item label="评价标签">
            <el-input v-model="actionDialog.form.tags" placeholder="多个标签用逗号分隔，如：响应及时,一次修复" />
          </el-form-item>
          <el-form-item label="匿名评价">
            <el-switch v-model="actionDialog.form.anonymous" />
          </el-form-item>
        </template>
      </el-form>
      <template #footer>
        <el-button @click="actionDialog.visible = false">取消</el-button>
        <el-button v-if="actionDialog.type === 'evaluate'" type="primary" @click="submitEvaluation">提交评价</el-button>
        <el-button v-else type="primary" :loading="submitting" @click="submitAction">确认</el-button>
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
  gap: 12px;
  flex-wrap: wrap;
}

.card-header__title {
  display: flex;
  align-items: center;
  gap: 10px;
  font-size: 16px;
  font-weight: 600;
}

.card-header__actions {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.ticket-no {
  color: var(--sf-primary);
  font-family: Consolas, Monaco, monospace;
}

.description-text {
  margin: 0;
  white-space: pre-wrap;
  font-family: inherit;
}

.record__title {
  display: flex;
  align-items: center;
  gap: 8px;
}

.record__operator {
  color: var(--sf-text-primary);
}

.record__hours {
  font-size: 12px;
  color: var(--sf-text-secondary);
}

.record__content {
  margin-top: 6px;
  color: var(--sf-text-secondary);
  white-space: pre-wrap;
}

.comment {
  padding: 12px 0;
  border-bottom: 1px dashed var(--sf-border-color);
}

.comment__meta {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 13px;
}

.comment__time {
  color: var(--sf-text-secondary);
}

.comment__content {
  margin-top: 6px;
  white-space: pre-wrap;
}

.comment-form {
  margin-top: 16px;
}

.comment-form__actions {
  margin-top: 10px;
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.upload-area {
  margin-top: 16px;
  display: flex;
  align-items: center;
  gap: 12px;
  flex-wrap: wrap;
}

.upload-hint {
  font-size: 12px;
  color: var(--sf-text-secondary);
}
</style>
