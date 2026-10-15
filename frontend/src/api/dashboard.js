import { get } from '@/utils/request'

export function fetchOverview() {
  return get('/dashboard/overview')
}

export function fetchTrend(params) {
  return get('/dashboard/trend', params)
}

export function fetchFaultDistribution() {
  return get('/dashboard/fault-distribution')
}

export function fetchPriorityDistribution() {
  return get('/dashboard/priority-distribution')
}

export function fetchEngineerWorkload() {
  return get('/dashboard/engineer-workload')
}

export function fetchSatisfaction() {
  return get('/dashboard/satisfaction')
}
