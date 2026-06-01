const ACCESS_TOKEN_KEY = 'exam_ai_access_token'
const REFRESH_TOKEN_KEY = 'exam_ai_refresh_token'

export function getAccessToken() {
  return localStorage.getItem(ACCESS_TOKEN_KEY) || ''
}

export function getRefreshToken() {
  return localStorage.getItem(REFRESH_TOKEN_KEY) || ''
}

export function setAuthTokens(tokenResponse) {
  if (tokenResponse?.accessToken) {
    localStorage.setItem(ACCESS_TOKEN_KEY, tokenResponse.accessToken)
  }
  if (tokenResponse?.refreshToken) {
    localStorage.setItem(REFRESH_TOKEN_KEY, tokenResponse.refreshToken)
  }
}

export function clearAuthTokens() {
  localStorage.removeItem(ACCESS_TOKEN_KEY)
  localStorage.removeItem(REFRESH_TOKEN_KEY)
}

export function isAuthenticated() {
  return Boolean(getAccessToken())
}
