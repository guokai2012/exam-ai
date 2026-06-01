import { api } from '../../api/http'

export function getCaptcha() {
  return api.get('/api/auth/captcha')
}

export function login(payload) {
  return api.post('/api/auth/login', payload)
}

export function register(payload) {
  return api.post('/api/auth/register', payload)
}

export function refresh(refreshToken) {
  return api.post('/api/auth/refresh', refreshToken ? { refreshToken } : {})
}

export function logout(refreshToken) {
  return api.post('/api/auth/logout', refreshToken ? { refreshToken } : {})
}

export function currentUser() {
  return api.get('/api/auth/me')
}

export function changePassword(payload) {
  return api.post('/api/auth/change-password', payload)
}
