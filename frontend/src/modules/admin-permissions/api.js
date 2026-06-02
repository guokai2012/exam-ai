import { api } from '../../api/http'
import { resolveMenuApiPath } from '../../shared/menuApiPath'

const ADMIN_PERMISSIONS_PAGE_PATH = '/admin/permissions'
const ADMIN_PERMISSIONS_DEFAULT_API = '/api/admin/permissions'

function adminPermissionApiPath() {
  return resolveMenuApiPath(ADMIN_PERMISSIONS_PAGE_PATH, ADMIN_PERMISSIONS_DEFAULT_API)
}

export function listPermissions() {
  return api.get(adminPermissionApiPath())
}

export function scanPermissions() {
  return api.post(`${adminPermissionApiPath()}/scan`)
}
