import { createRouter, createWebHistory } from 'vue-router'
import { isAuthenticated } from '../shared/authToken'

const AppLayout = () => import('../layouts/AppLayout.vue')
const AdminMenusPage = () => import('../modules/admin-menus/AdminMenusPage.vue')
const AdminPermissionsPage = () => import('../modules/admin-permissions/AdminPermissionsPage.vue')
const AdminRolesPage = () => import('../modules/admin-roles/AdminRolesPage.vue')
const AdminUsersPage = () => import('../modules/admin-users/AdminUsersPage.vue')
const LoginPage = () => import('../modules/auth/LoginPage.vue')
const RegisterPage = () => import('../modules/auth/RegisterPage.vue')
const ChangePasswordPage = () => import('../modules/change-password/ChangePasswordPage.vue')
const DocumentsPage = () => import('../modules/documents/DocumentsPage.vue')
const NotificationsPage = () => import('../modules/notifications/NotificationsPage.vue')
const ProfilePage = () => import('../modules/profile/ProfilePage.vue')
const AvailableQuestionsPage = () => import('../modules/questions/AvailableQuestionsPage.vue')
const PendingConfirmQuestionsPage = () => import('../modules/questions/PendingConfirmQuestionsPage.vue')
const SystemConfigPage = () => import('../modules/system-config/SystemConfigPage.vue')

export const appRoutes = [
    {
      path: '/login',
      name: 'login',
      component: LoginPage,
      meta: { public: true, title: '登录' }
    },
    {
      path: '/register',
      component: RegisterPage,
      meta: { public: true, title: '注册' }
    },
    {
      path: '/change-password',
      component: ChangePasswordPage,
      meta: { title: '修改密码' }
    },
    {
      path: '/',
      component: AppLayout,
      redirect: '/documents',
      children: [
        {
          path: 'documents',
          component: DocumentsPage,
          meta: {
            title: '我的文档',
            eyebrow: 'My Documents',
            menu: true,
            menuKey: 'menu:/documents',
            menuName: '我的文档',
            icon: 'Document',
            sortOrder: 10,
            apiPath: '/api/documents',
            permissionCode: 'document:list'
          }
        },
        {
          path: 'questions',
          redirect: '/questions/available',
          meta: {
            title: '题库管理',
            menu: true,
            menuGroup: true,
            menuKey: 'group:/questions',
            menuName: '题库管理',
            icon: 'Collection',
            sortOrder: 20
          }
        },
        {
          path: 'questions/available',
          component: AvailableQuestionsPage,
          meta: {
            title: '可用题',
            eyebrow: 'Available Questions',
            menu: true,
            parentMenuKey: 'group:/questions',
            menuKey: 'menu:/questions/available',
            menuName: '可用题',
            icon: 'Collection',
            sortOrder: 10,
            apiPath: '/api/questions',
            permissionCode: 'question:list'
          }
        },
        {
          path: 'questions/pending-confirm',
          component: PendingConfirmQuestionsPage,
          meta: {
            title: '待确认题',
            eyebrow: 'Pending Confirmation',
            menu: true,
            parentMenuKey: 'group:/questions',
            menuKey: 'menu:/questions/pending-confirm',
            menuName: '待确认题',
            icon: 'EditPen',
            sortOrder: 20,
            apiPath: '/api/questions',
            permissionCode: 'question:list'
          }
        },
        {
          path: 'admin',
          redirect: '/admin/users',
          meta: {
            title: '后台管理',
            menu: true,
            menuGroup: true,
            menuKey: 'group:/admin',
            menuName: '后台管理',
            icon: 'Setting',
            sortOrder: 30
          }
        },
        {
          path: 'admin/users',
          component: AdminUsersPage,
          meta: {
            title: '用户管理',
            eyebrow: 'Admin Users',
            menu: true,
            parentMenuKey: 'group:/admin',
            menuKey: 'menu:/admin/users',
            menuName: '用户管理',
            icon: 'User',
            sortOrder: 10,
            apiPath: '/api/admin/users',
            permissionCode: 'admin:user:list'
          }
        },
        {
          path: 'admin/roles',
          component: AdminRolesPage,
          meta: {
            title: '角色管理',
            eyebrow: 'Admin Roles',
            menu: true,
            parentMenuKey: 'group:/admin',
            menuKey: 'menu:/admin/roles',
            menuName: '角色管理',
            icon: 'UserFilled',
            sortOrder: 20,
            apiPath: '/api/admin/roles',
            permissionCode: 'admin:role:list'
          }
        },
        {
          path: 'admin/permissions',
          component: AdminPermissionsPage,
          meta: {
            title: '权限管理',
            eyebrow: 'Admin Permissions',
            menu: true,
            parentMenuKey: 'group:/admin',
            menuKey: 'menu:/admin/permissions',
            menuName: '权限管理',
            icon: 'Key',
            sortOrder: 30,
            apiPath: '/api/admin/permissions',
            permissionCode: 'admin:permission:list'
          }
        },
        {
          path: 'admin/menus',
          component: AdminMenusPage,
          meta: {
            title: '菜单管理',
            eyebrow: 'Admin Menus',
            menu: true,
            parentMenuKey: 'group:/admin',
            menuKey: 'menu:/admin/menus',
            menuName: '菜单管理',
            icon: 'Menu',
            sortOrder: 40,
            apiPath: '/api/admin/menus',
            permissionCode: 'admin:menu:list'
          }
        },
        {
          path: 'system-configs',
          component: SystemConfigPage,
          meta: {
            title: '系统配置',
            eyebrow: 'System Config',
            menu: true,
            menuKey: 'menu:/system-configs',
            menuName: '系统配置',
            icon: 'Setting',
            sortOrder: 40,
            apiPath: '/api/system-configs',
            permissionCode: 'system-config:list'
          }
        },
        {
          path: 'notifications',
          component: NotificationsPage,
          meta: {
            title: '站内通知',
            eyebrow: 'Notifications',
            menu: true,
            menuKey: 'menu:/notifications',
            menuName: '站内通知',
            icon: 'Bell',
            sortOrder: 50,
            apiPath: '/api/notifications',
            permissionCode: 'notification:list'
          }
        },
        {
          path: 'profile',
          component: ProfilePage,
          meta: {
            title: '用户详情',
            eyebrow: 'Profile',
            menu: true,
            menuKey: 'menu:/profile',
            menuName: '用户详情',
            icon: 'User',
            sortOrder: 60
          }
        }
      ]
    }
]

const router = createRouter({
  history: createWebHistory(),
  routes: appRoutes
})

router.beforeEach((to) => {
  if (to.meta.public && isAuthenticated()) {
    return '/documents'
  }
  if (!to.meta.public && !isAuthenticated()) {
    return { path: '/login', query: { redirect: to.fullPath } }
  }
  return true
})

export default router
