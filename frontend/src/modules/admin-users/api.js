import { api } from '../../api/http'
import { PAGE_DEFAULTS } from '../../shared/constants'
import { resolveMenuApiPath } from '../../shared/menuApiPath'

const ADMIN_USERS_PAGE_PATH = '/admin/users'
const ADMIN_USERS_DEFAULT_API = '/api/admin/users'

function adminUserApiPath() {
  return resolveMenuApiPath(ADMIN_USERS_PAGE_PATH, ADMIN_USERS_DEFAULT_API)
}

export function listUsers(keyword = '', page = PAGE_DEFAULTS.page, size = PAGE_DEFAULTS.size) {
  const params = new URLSearchParams({ page: String(page), size: String(size) })
  if (keyword) params.set('keyword', keyword)
  return api.get(`${adminUserApiPath()}?${params.toString()}`)
}

export function createUser(payload) {
  return api.post(adminUserApiPath(), payload)
}

export function updateUser(id, payload) {
  return api.put(`${adminUserApiPath()}/${id}`, payload)
}

export function disableUser(id) {
  return api.delete(`${adminUserApiPath()}/${id}`)
}

export function kickUser(id) {
  return api.post(`${adminUserApiPath()}/${id}/kick`)
}

export function resetPassword(id, password) {
  return api.post(`${adminUserApiPath()}/${id}/reset-password`, { password })
}
