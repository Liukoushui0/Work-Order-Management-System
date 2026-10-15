import { del, get, post, put } from '@/utils/request'

export function fetchCustomers(params) {
  return get('/customers', params)
}

export function fetchCustomerOptions(keyword) {
  return get('/customers/options', keyword ? { keyword } : undefined)
}

export function fetchCustomerDetail(id) {
  return get(`/customers/${id}`)
}

export function createCustomer(data) {
  return post('/customers', data)
}

export function updateCustomer(id, data) {
  return put(`/customers/${id}`, data)
}

export function updateCustomerStatus(id, status) {
  return put(`/customers/${id}/status`, { status })
}

export function deleteCustomer(id) {
  return del(`/customers/${id}`)
}

export function fetchDevices(params) {
  return get('/devices', params)
}

export function fetchDeviceOptions(customerId) {
  return get('/devices/options', { customerId })
}

export function createDevice(data) {
  return post('/devices', data)
}

export function updateDevice(id, data) {
  return put(`/devices/${id}`, data)
}

export function deleteDevice(id) {
  return del(`/devices/${id}`)
}
