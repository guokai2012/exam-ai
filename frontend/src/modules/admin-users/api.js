import { api } from '../../api/http'
import { PAGE_DEFAULTS } from '../../shared/constants'

export function listUsers(keyword = '', page = PAGE_DEFAULTS.page, size = PAGE_DEFAULTS.size) {
  const params = new URLSearchParams({ page: String(page), size: String(size) })
  if (keyword) params.set('keyword', keyword)
  return api.get(`/api/admin/users?${params.toString()}`)
}

export function createUser(payload) {
  return api.post('/api/admin/users', payload)
}

export function updateUser(id, payload) {
  return api.put(`/api/admin/users/${id}`, payload)
}

export function disableUser(id) {
  return api.delete(`/api/admin/users/${id}`)
}

export function kickUser(id) {
  return api.post(`/api/admin/users/${id}/kick`)
}

export function resetPassword(id, password) {
  return api.post(`/api/admin/users/${id}/reset-password`, { password })
}
