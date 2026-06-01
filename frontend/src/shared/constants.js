/**
 * 前端通用业务常量，集中维护分页、表单校验和菜单组件约束，避免页面散落魔法值。
 */
export const PAGE_DEFAULTS = {
  page: 1,
  size: 20,
  documentSize: 10,
  sizes: [10, 20, 50]
}

export const VALIDATION_LIMITS = {
  usernameMin: 3,
  passwordMin: 6,
  passwordStrongMin: 8,
  accountMax: 64
}

export const USER_STATUS = {
  enabled: 1,
  disabled: 0
}

export const AUTH_REFRESH_SKIP_PATHS = [
  '/api/auth/captcha',
  '/api/auth/register',
  '/api/auth/login',
  '/api/auth/refresh'
]

export const MENU_COMPONENT_OPTIONS = [
  { label: '菜单分组', value: 'MenuGroup' },
  { label: '我的文档', value: 'DocumentsPage' },
  { label: '可用题', value: 'AvailableQuestionsPage' },
  { label: '待确认题', value: 'PendingConfirmQuestionsPage' },
  { label: '站内通知', value: 'NotificationsPage' },
  { label: '用户详情', value: 'ProfilePage' },
  { label: '系统配置', value: 'SystemConfigPage' },
  { label: '用户管理', value: 'AdminUsersPage' },
  { label: '角色管理', value: 'AdminRolesPage' },
  { label: '权限管理', value: 'AdminPermissionsPage' },
  { label: '菜单管理', value: 'AdminMenusPage' }
]
