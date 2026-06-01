import { clearAuthTokens, getAccessToken } from '../shared/authToken'

export class ApiError extends Error {
  constructor(message, status, code) {
    super(message)
    this.name = 'ApiError'
    this.status = status
    this.code = code
  }
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
  if (response.status === 401) {
    clearAuthTokens()
    if (window.location.pathname !== '/login') {
      window.location.href = `/login?redirect=${encodeURIComponent(window.location.pathname + window.location.search)}`
    }
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
