/** 工单状态与优先级的展示映射（与后端枚举保持一致） */
export const TICKET_STATUS_OPTIONS = [
  { value: 'PENDING_ASSIGN', label: '待分派', type: 'info' },
  { value: 'PENDING_ACCEPT', label: '待接单', type: 'warning' },
  { value: 'PROCESSING', label: '处理中', type: 'primary' },
  { value: 'PENDING_CONFIRM', label: '待确认', type: 'warning' },
  { value: 'COMPLETED', label: '已完成', type: 'success' },
  { value: 'CANCELLED', label: '已取消', type: 'info' }
]

export const TICKET_PRIORITY_OPTIONS = [
  { value: 'LOW', label: '低', type: 'info' },
  { value: 'MEDIUM', label: '中', type: 'primary' },
  { value: 'HIGH', label: '高', type: 'warning' },
  { value: 'URGENT', label: '紧急', type: 'danger' }
]

export const TICKET_SOURCE_OPTIONS = [
  { value: 'PHONE', label: '电话' },
  { value: 'WEB', label: '官网' },
  { value: 'WECHAT', label: '微信' },
  { value: 'EMAIL', label: '邮件' }
]

export const SATISFACTION_LABELS = {
  VERY_SATISFIED: '非常满意',
  SATISFIED: '满意',
  NEUTRAL: '一般',
  UNSATISFIED: '不满意',
  VERY_UNSATISFIED: '非常不满意'
}

export const ATTACHMENT_BIZ_TYPES = [
  { value: 'FAULT_PHOTO', label: '故障照片' },
  { value: 'REPAIR_PHOTO', label: '维修照片' },
  { value: 'REPORT', label: '维修报告' },
  { value: 'OTHER', label: '其他' }
]

export const CUSTOMER_LEVEL_OPTIONS = ['S', 'A', 'B', 'C'].map(level => ({ value: level, label: `${level} 级` }))

function labelOf(options, value, fallback = '-') {
  const matched = options.find(item => item.value === value)
  return matched ? matched.label : (value || fallback)
}

export function statusLabel(status) {
  return labelOf(TICKET_STATUS_OPTIONS, status)
}

export function statusTagType(status) {
  const matched = TICKET_STATUS_OPTIONS.find(item => item.value === status)
  return matched ? matched.type : 'info'
}

export function priorityLabel(priority) {
  return labelOf(TICKET_PRIORITY_OPTIONS, priority)
}

export function priorityTagType(priority) {
  const matched = TICKET_PRIORITY_OPTIONS.find(item => item.value === priority)
  return matched ? matched.type : 'info'
}

export function sourceLabel(source) {
  return labelOf(TICKET_SOURCE_OPTIONS, source)
}

export function satisfactionLabel(satisfaction) {
  return SATISFACTION_LABELS[satisfaction] || satisfaction || '-'
}

/** 日期时间展示：后端返回 yyyy-MM-dd HH:mm:ss 字符串 */
export function formatDateTime(value) {
  if (!value) return '-'
  return String(value).replace('T', ' ')
}

export function formatDate(value) {
  if (!value) return '-'
  return String(value).slice(0, 10)
}

export function formatFileSize(bytes) {
  if (bytes === null || bytes === undefined) return '-'
  const size = Number(bytes)
  if (size < 1024) return `${size} B`
  if (size < 1024 * 1024) return `${(size / 1024).toFixed(1)} KB`
  return `${(size / 1024 / 1024).toFixed(2)} MB`
}

/** 是否逾期：未结束且已超过截止时间 */
export function isOverdue(ticket) {
  if (!ticket || !ticket.deadlineTime) return false
  if (ticket.status === 'COMPLETED' || ticket.status === 'CANCELLED') return false
  return new Date(String(ticket.deadlineTime).replace(' ', 'T')).getTime() < Date.now()
}
