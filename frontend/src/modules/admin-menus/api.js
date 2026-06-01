import { api } from '../../api/http'

export function myMenus() {
  return api.get('/api/menus/me')
}

export function listMenus() {
  return api.get('/api/admin/menus')
}

export function createMenu(payload) {
  return api.post('/api/admin/menus', payload)
}

export function updateMenu(id, payload) {
  return api.put(`/api/admin/menus/${id}`, payload)
}

export function deleteMenu(id) {
  return api.delete(`/api/admin/menus/${id}`)
}
