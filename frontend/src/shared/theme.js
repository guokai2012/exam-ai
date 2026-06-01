const THEME_KEY = 'exam_ai_theme'

export const themes = [
  { name: 'blue', label: '默认蓝', color: '#2563eb' },
  { name: 'green', label: '绿色', color: '#059669' },
  { name: 'dark', label: '深色', color: '#1f2937' }
]

export function getTheme() {
  return localStorage.getItem(THEME_KEY) || 'blue'
}

export function setTheme(themeName) {
  localStorage.setItem(THEME_KEY, themeName)
  applyTheme(themeName)
}

export function applyTheme(themeName = getTheme()) {
  document.documentElement.dataset.theme = themeName
}

export function themeLabel(themeName) {
  return themes.find((theme) => theme.name === themeName)?.label || '默认蓝'
}
