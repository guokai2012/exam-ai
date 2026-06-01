import { api } from '../../api/http'
import { PAGE_DEFAULTS } from '../../shared/constants'

export function listNotifications(page = PAGE_DEFAULTS.page, size = PAGE_DEFAULTS.size) {
  return api.get(`/api/notifications?page=${page}&size=${size}`)
}

export function markNotificationRead(id) {
  return api.post(`/api/notifications/${id}/read`)
}
