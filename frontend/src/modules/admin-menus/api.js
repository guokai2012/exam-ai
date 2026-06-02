import { api } from '../../api/http'
import { resolveMenuApiPath } from '../../shared/menuApiPath'

const ADMIN_MENUS_PAGE_PATH = '/admin/menus'
const ADMIN_MENUS_DEFAULT_API = '/api/admin/menus'

function adminMenuApiPath() {
  return resolveMenuApiPath(ADMIN_MENUS_PAGE_PATH, ADMIN_MENUS_DEFAULT_API)
}

export function myMenus() {
  return api.get('/api/menus/me')
}

export function listMenus() {
  return api.get(adminMenuApiPath())
}

export function createMenu(payload) {
  return api.post(adminMenuApiPath(), payload)
}

export function updateMenu(id, payload) {
  return api.put(`${adminMenuApiPath()}/${id}`, payload)
}

export function deleteMenu(id) {
  return api.delete(`${adminMenuApiPath()}/${id}`)
}

export function listApiPathOptions() {
  return api.get(`${adminMenuApiPath()}/api-path-options`)
}

export function requestMenuScanToken() {
  return api.post(`${adminMenuApiPath()}/scan-token`, {})
}

export function syncScannedMenus(payload, token) {
  return api.postWithHeaders(`${adminMenuApiPath()}/scan-sync`, payload, {
    'X-Menu-Scan-Token': token
  })
}
