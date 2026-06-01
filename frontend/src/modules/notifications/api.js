import { api } from '../../api/http'

export function listNotifications(page = 1, size = 20) {
  return api.get(`/api/notifications?page=${page}&size=${size}`)
}

export function markNotificationRead(id) {
  return api.post(`/api/notifications/${id}/read`)
}
