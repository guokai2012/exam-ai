import { api } from '../../api/http'
import { PAGE_DEFAULTS } from '../../shared/constants'
import { resolveMenuApiPath } from '../../shared/menuApiPath'

const NOTIFICATIONS_PAGE_PATH = '/notifications'
const NOTIFICATIONS_DEFAULT_API = '/api/notifications'

function notificationApiPath() {
  return resolveMenuApiPath(NOTIFICATIONS_PAGE_PATH, NOTIFICATIONS_DEFAULT_API)
}

export function listNotifications(page = PAGE_DEFAULTS.page, size = PAGE_DEFAULTS.size) {
  return api.get(`${notificationApiPath()}?page=${page}&size=${size}`)
}

export function markNotificationRead(id) {
  return api.post(`${notificationApiPath()}/${id}/read`)
}
