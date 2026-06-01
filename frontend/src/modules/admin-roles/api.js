import { api } from '../../api/http'

export function listRoles() {
  return api.get('/api/admin/roles')
}

export function createRole(payload) {
  return api.post('/api/admin/roles', payload)
}

export function updateRole(id, payload) {
  return api.put(`/api/admin/roles/${id}`, payload)
}

export function deleteRole(id) {
  return api.delete(`/api/admin/roles/${id}`)
}
