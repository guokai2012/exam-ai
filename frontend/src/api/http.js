import { clearAuthTokens, getAccessToken, getRefreshToken, setAuthTokens } from '../shared/authToken'

let refreshPromise = null
const AUTH_REFRESH_SKIP_PATHS = ['/api/auth/captcha', '/api/auth/register', '/api/auth/login', '/api/auth/refresh']

export class ApiError extends Error {
  constructor(message, status, code) {
    super(message)
    this.name = 'ApiError'
    this.status = status
    this.code = code
  }
}

function redirectToLogin() {
  clearAuthTokens()
  if (window.location.pathname !== '/login') {
    window.location.href = `/login?redirect=${encodeURIComponent(window.location.pathname + window.location.search)}`
  }
}

async function refreshAccessToken() {
  if (!refreshPromise) {
    refreshPromise = fetch('/api/auth/refresh', {
      method: 'POST',
      credentials: 'include',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify(getRefreshToken() ? { refreshToken: getRefreshToken() } : {})
    })
      .then(async (response) => {
        const payload = await response.json().catch(() => null)
        if (!response.ok) {
          throw new ApiError(payload?.message || '登录已过期', response.status, payload?.code)
        }
        setAuthTokens(payload?.data)
        return payload?.data
      })
      .finally(() => {
        refreshPromise = null
      })
  }
  return refreshPromise
}

async function requestPayload(path, options = {}) {
  const token = getAccessToken()
  const headers = { ...(options.headers || {}) }
  const init = {
    method: options.method || 'GET',
    credentials: 'include',
    headers
  }

  if (token) {
    headers.Authorization = `Bearer ${token}`
  }

  if (options.body instanceof FormData) {
    init.body = options.body
  } else if (options.body !== undefined) {
    headers['Content-Type'] = 'application/json'
    init.body = JSON.stringify(options.body)
  }

  const response = await fetch(path, init)
  const payload = await response.json().catch(() => null)
  if (response.status === 401 && !options.skipAuthRefresh && !options.retried && !AUTH_REFRESH_SKIP_PATHS.includes(path)) {
    try {
      await refreshAccessToken()
      return requestPayload(path, { ...options, retried: true })
    } catch {
      redirectToLogin()
    }
  }
  if (response.status === 401) {
    redirectToLogin()
  }
  if (!response.ok) {
    if (payload?.code === 'PASSWORD_CHANGE_REQUIRED' && window.location.pathname !== '/change-password') {
      window.location.href = '/change-password'
    }
    throw new ApiError(payload?.message || '请求失败', response.status, payload?.code)
  }
  return payload
}

export async function request(path, options = {}) {
  return (await requestPayload(path, options))?.data
}

export async function requestResult(path, options = {}) {
  return requestPayload(path, options)
}

export const api = {
  get(path) {
    return request(path)
  },
  post(path, body) {
    return request(path, { method: 'POST', body })
  },
  put(path, body) {
    return request(path, { method: 'PUT', body })
  },
  putResult(path, body) {
    return requestResult(path, { method: 'PUT', body })
  },
  delete(path) {
    return request(path, { method: 'DELETE' })
  },
  upload(path, formData) {
    return request(path, { method: 'POST', body: formData })
  }
}
