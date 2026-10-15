import { del, get, post } from '@/utils/request'

/** 工单分页列表 */
export function fetchTickets(params) {
  return get('/tickets', params)
}

/** 逾期工单列表 */
export function fetchOverdueTickets(params) {
  return get('/tickets/overdue', params)
}

/** 工单详情 */
export function fetchTicketDetail(id) {
  return get(`/tickets/${id}`)
}

/** 工单处理历史 */
export function fetchTicketRecords(id) {
  return get(`/tickets/${id}/records`)
}

export function createTicket(data) {
  return post('/tickets', data)
}

export function assignTicket(id, data) {
  return post(`/tickets/${id}/assign`, data)
}

export function acceptTicket(id) {
  return post(`/tickets/${id}/accept`)
}

export function transferTicket(id, data) {
  return post(`/tickets/${id}/transfer`, data)
}

export function rejectTicket(id, data) {
  return post(`/tickets/${id}/reject`, data)
}

export function processTicket(id, data) {
  return post(`/tickets/${id}/process`, data)
}

export function completeTicket(id, data) {
  return post(`/tickets/${id}/complete`, data)
}

export function confirmTicket(id, data) {
  return post(`/tickets/${id}/confirm`, data)
}

export function reopenTicket(id, data) {
  return post(`/tickets/${id}/reopen`, data)
}

export function cancelTicket(id, data) {
  return post(`/tickets/${id}/cancel`, data)
}

/** 工单评论 */
export function fetchComments(id) {
  return get(`/tickets/${id}/comments`)
}

export function createComment(id, data) {
  return post(`/tickets/${id}/comments`, data)
}

/** 附件 */
export function fetchAttachments(id) {
  return get(`/tickets/${id}/attachments`)
}

export function uploadAttachment(id, file, bizType) {
  const form = new FormData()
  form.append('file', file)
  if (bizType) {
    form.append('bizType', bizType)
  }
  return post(`/tickets/${id}/attachments`, form, {
    headers: { 'Content-Type': 'multipart/form-data' }
  })
}

export function deleteAttachment(id, attachmentId) {
  return del(`/tickets/${id}/attachments/${attachmentId}`)
}

/** 服务评价 */
export function fetchEvaluation(id) {
  return get(`/tickets/${id}/evaluation`)
}

export function createEvaluation(id, data) {
  return post(`/tickets/${id}/evaluation`, data)
}
