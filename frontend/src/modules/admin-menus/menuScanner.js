import { appRoutes } from '../../router'
import { USER_STATUS } from '../../shared/constants'

const ROOT_PATH = '/'

/**
 * 扫描前端路由中的菜单元数据，生成后端可同步的菜单树。
 *
 * @returns {Object} 菜单同步请求体。
 */
export function scanRouterMenus() {
  const items = flattenMenuRoutes(appRoutes, ROOT_PATH)
  const byKey = new Map(items.map(item => [item.menuKey, { ...item, children: [] }]))
  const roots = []

  for (const item of byKey.values()) {
    if (item.parentMenuKey && byKey.has(item.parentMenuKey)) {
      byKey.get(item.parentMenuKey).children.push(item)
    } else {
      roots.push(item)
    }
  }

  sortMenus(roots)
  return { menus: cleanupMenus(roots) }
}

/**
 * 展平路由表中声明为菜单的路由。
 *
 * @param {Array} routes Vue Router 路由配置。
 * @param {string} parentPath 父级路由路径。
 * @returns {Array} 平铺菜单节点。
 */
function flattenMenuRoutes(routes, parentPath) {
  return routes.flatMap(route => {
    const currentPath = resolveRoutePath(parentPath, route.path)
    const children = flattenMenuRoutes(route.children || [], currentPath)
    if (!route.meta?.menu) {
      return children
    }
    return [
      {
        menuKey: route.meta.menuKey,
        parentMenuKey: route.meta.parentMenuKey || null,
        menuName: route.meta.menuName || route.meta.title,
        path: route.meta.menuGroup ? null : currentPath,
        apiPath: route.meta.menuGroup ? null : route.meta.apiPath || null,
        icon: route.meta.icon || null,
        sortOrder: Number(route.meta.sortOrder || 0),
        status: USER_STATUS.enabled,
        permissionCode: route.meta.menuGroup ? null : route.meta.permissionCode || null
      },
      ...children
    ]
  })
}

/**
 * 解析子路由在浏览器中的绝对路径。
 *
 * @param {string} parentPath 父级路径。
 * @param {string} routePath 当前路由路径。
 * @returns {string} 绝对路径。
 */
function resolveRoutePath(parentPath, routePath) {
  if (routePath.startsWith('/')) {
    return routePath
  }
  const base = parentPath === ROOT_PATH ? '' : parentPath
  return `${base}/${routePath}`.replace(/\/+/g, '/')
}

/**
 * 按排序值稳定排列菜单树。
 *
 * @param {Array} menus 菜单树。
 */
function sortMenus(menus) {
  menus.sort((left, right) => left.sortOrder - right.sortOrder || left.menuName.localeCompare(right.menuName))
  menus.forEach(menu => sortMenus(menu.children))
}

/**
 * 移除仅供前端组树使用的 parentMenuKey 字段。
 *
 * @param {Array} menus 菜单树。
 * @returns {Array} 后端同步菜单树。
 */
function cleanupMenus(menus) {
  return menus.map(({ parentMenuKey, ...menu }) => ({
    ...menu,
    children: cleanupMenus(menu.children || [])
  }))
}
