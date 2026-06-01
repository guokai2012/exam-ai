import { api } from '../../api/http'

export function listPermissions() {
  return api.get('/api/admin/permissions')
}

export function createPermission(payload) {
  return api.post('/api/admin/permissions', payload)
}

export function scanPermissions() {
  return api.post('/api/admin/permissions/scan')
}

export function updatePermission(id, payload) {
  return api.put(`/api/admin/permissions/${id}`, payload)
}

export function deletePermission(id) {
  return api.delete(`/api/admin/permissions/${id}`)
}
