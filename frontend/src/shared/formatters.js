export function formatSize(size) {
  if (!size) return '0 B'
  if (size < 1024) return `${size} B`
  if (size < 1024 * 1024) return `${(size / 1024).toFixed(1)} KB`
  return `${(size / 1024 / 1024).toFixed(1)} MB`
}

export function typeLabel(type) {
  return {
    SINGLE_CHOICE: '单选题',
    MULTIPLE_CHOICE: '多选题',
    TRUE_FALSE: '判断题',
    SHORT_ANSWER: '简答题'
  }[type] || type || '-'
}

export function stateLabel(status) {
  return {
    PARSE_PENDING_CONFIRM: '解析待确认',
    PARSE_REJECTED: '解析已驳回',
    TAG_PENDING: '待 AI 标签',
    TAG_PROCESSING: '标签分析中',
    TAG_FAILED: '标签失败',
    AVAILABLE: '可用',
    UPLOADED: '已上传',
    PARSING: '解析中',
    PARSE_PARTIAL_FAILED: '部分解析失败',
    PARSE_FAILED: '解析失败',
    PENDING_CONFIRMATION: '待确认解析结果',
    CONFIRMED: '已确认'
  }[status] || status || '-'
}

export function roleLabel(role) {
  return {
    ADMIN: '管理员',
    TEACHER: '老师',
    STUDENT: '学生'
  }[role] || role
}

export function roleSummary(roles = []) {
  return roles.length ? roles.map(roleLabel).join(' / ') : '未分配角色'
}

export function canAnalyzeDocument(status) {
  return ['UPLOADED', 'PARSE_FAILED', 'PARSE_PARTIAL_FAILED'].includes(status)
}

export function analyzeButtonText(status) {
  return ['PARSE_FAILED', 'PARSE_PARTIAL_FAILED'].includes(status) ? '继续解析' : 'AI 解析'
}
