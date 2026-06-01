import { clearAuthTokens, getAccessToken, getRefreshToken, setAuthTokens } from '../shared/authToken'
import { AUTH_REFRESH_SKIP_PATHS } from '../shared/constants'

let refreshPromise = null

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

/**
 * 使用 refresh token 换取新的 access token。
 * 多个接口同时遇到 401 时共享同一个刷新 Promise，避免并发刷新导致后端令牌轮换冲突。
 */
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

/**
 * 统一发送接口请求，负责注入访问令牌、序列化请求体、处理统一响应和登录态续期。
 * 当业务接口返回 401 时会先尝试刷新令牌并重放原请求，刷新失败才跳转登录页。
 */
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
    // 文件上传必须保留浏览器生成的 multipart boundary，不能手动指定 JSON Content-Type。
    init.body = options.body
  } else if (options.body !== undefined) {
    headers['Content-Type'] = 'application/json'
    init.body = JSON.stringify(options.body)
  }

  const response = await fetch(path, init)
  const payload = await response.json().catch(() => null)
  if (response.status === 401 && !options.skipAuthRefresh && !options.retried && !AUTH_REFRESH_SKIP_PATHS.includes(path)) {
    try {
      // 刷新成功后重放原请求，并用 retried 标记避免异常情况下无限重试。
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
