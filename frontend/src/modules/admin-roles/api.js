import { api } from '../../api/http'
import { resolveMenuApiPath } from '../../shared/menuApiPath'

const ADMIN_ROLES_PAGE_PATH = '/admin/roles'
const ADMIN_ROLES_DEFAULT_API = '/api/admin/roles'

function adminRoleApiPath() {
  return resolveMenuApiPath(ADMIN_ROLES_PAGE_PATH, ADMIN_ROLES_DEFAULT_API)
}

export function listRoles() {
  return api.get(adminRoleApiPath())
}

export function createRole(payload) {
  return api.post(adminRoleApiPath(), payload)
}

export function updateRole(id, payload) {
  return api.put(`${adminRoleApiPath()}/${id}`, payload)
}

export function deleteRole(id) {
  return api.delete(`${adminRoleApiPath()}/${id}`)
}
