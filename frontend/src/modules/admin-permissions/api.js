import { api } from '../../api/http'

export function listPermissions() {
  return api.get('/api/admin/permissions')
}

export function scanPermissions() {
  return api.post('/api/admin/permissions/scan')
}
