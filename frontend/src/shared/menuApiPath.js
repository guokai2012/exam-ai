import { ref } from 'vue'

const menuTree = ref([])

/**
 * 更新当前登录用户可见菜单树，供页面 API 模块解析菜单绑定的 apiPath。
 *
 * @param {Array} menus 后端返回的菜单树。
 */
export function setMenuApiPathSource(menus) {
  menuTree.value = Array.isArray(menus) ? menus : []
}

/**
 * 根据页面路由解析菜单绑定的 API 根路径，缺失配置时使用模块默认值兜底。
 *
 * @param {string|string[]} pagePaths 当前模块可能对应的前端页面路径。
 * @param {string} fallbackApiPath 模块默认 API 根路径。
 * @returns {string} 可用于发起请求的 API 根路径。
 */
export function resolveMenuApiPath(pagePaths, fallbackApiPath) {
  const candidates = normalizeCandidates(pagePaths)
  const currentPath = window.location?.pathname
  const orderedPaths = prioritizeCurrentCandidate(candidates, currentPath)
  const menus = flattenMenus(menuTree.value)
  const matched = orderedPaths
    .map(path => menus.find(menu => menu.path === path && menu.apiPath))
    .find(Boolean)

  // 菜单由后端权限过滤，未加载或未授权时仍使用默认路径保障页面基础可用。
  return matched?.apiPath || fallbackApiPath
}

/**
 * 展平菜单树，便于通过页面 path 查找绑定的 apiPath。
 *
 * @param {Array} menus 菜单树。
 * @returns {Array} 展平后的菜单节点。
 */
function flattenMenus(menus) {
  return menus.flatMap(menu => [menu, ...flattenMenus(menu.children || [])])
}

/**
 * 统一把单个页面路径或多个页面路径转换为数组。
 *
 * @param {string|string[]} pagePaths 页面路径。
 * @returns {string[]} 页面路径数组。
 */
function normalizeCandidates(pagePaths) {
  return Array.isArray(pagePaths) ? pagePaths : [pagePaths]
}

/**
 * 仅当当前路由属于调用方声明的候选页面时才提升优先级。
 *
 * @param {string[]} candidates API 模块声明的页面路径候选。
 * @param {string} currentPath 当前浏览器页面路径。
 * @returns {string[]} 去重后的查询顺序。
 */
function prioritizeCurrentCandidate(candidates, currentPath) {
  if (!currentPath || !candidates.includes(currentPath)) {
    return candidates
  }

  // API 模块可能被其他页面复用，不能用非候选当前路由覆盖模块自己的 apiPath。
  return [currentPath, ...candidates.filter(path => path !== currentPath)]
}
