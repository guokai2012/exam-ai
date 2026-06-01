import { createRouter, createWebHistory } from 'vue-router'
import { isAuthenticated } from '../shared/authToken'

const AppLayout = () => import('../layouts/AppLayout.vue')
const AdminMenusPage = () => import('../modules/admin-menus/AdminMenusPage.vue')
const AdminPermissionsPage = () => import('../modules/admin-permissions/AdminPermissionsPage.vue')
const AdminRolesPage = () => import('../modules/admin-roles/AdminRolesPage.vue')
const AdminUsersPage = () => import('../modules/admin-users/AdminUsersPage.vue')
const LoginPage = () => import('../modules/auth/LoginPage.vue')
const ChangePasswordPage = () => import('../modules/change-password/ChangePasswordPage.vue')
const DocumentsPage = () => import('../modules/documents/DocumentsPage.vue')
const NotificationsPage = () => import('../modules/notifications/NotificationsPage.vue')
const ProfilePage = () => import('../modules/profile/ProfilePage.vue')
const AvailableQuestionsPage = () => import('../modules/questions/AvailableQuestionsPage.vue')
const PendingConfirmQuestionsPage = () => import('../modules/questions/PendingConfirmQuestionsPage.vue')
const SystemConfigPage = () => import('../modules/system-config/SystemConfigPage.vue')

const router = createRouter({
  history: createWebHistory(),
  routes: [
    {
      path: '/login',
      name: 'login',
      component: LoginPage,
      meta: { public: true, title: '登录' }
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
        { path: 'documents', component: DocumentsPage, meta: { title: '我的文档', eyebrow: 'My Documents' } },
        { path: 'questions', redirect: '/questions/available' },
        { path: 'questions/available', component: AvailableQuestionsPage, meta: { title: '可用题', eyebrow: 'Available Questions' } },
        { path: 'questions/pending-confirm', component: PendingConfirmQuestionsPage, meta: { title: '待确认题', eyebrow: 'Pending Confirmation' } },
        { path: 'system-configs', component: SystemConfigPage, meta: { title: '系统配置', eyebrow: 'System Config' } },
        { path: 'notifications', component: NotificationsPage, meta: { title: '站内通知', eyebrow: 'Notifications' } },
        { path: 'profile', component: ProfilePage, meta: { title: '用户详情', eyebrow: 'Profile' } },
        { path: 'admin/users', component: AdminUsersPage, meta: { title: '用户管理', eyebrow: 'Admin Users' } },
        { path: 'admin/roles', component: AdminRolesPage, meta: { title: '角色管理', eyebrow: 'Admin Roles' } },
        { path: 'admin/permissions', component: AdminPermissionsPage, meta: { title: '权限管理', eyebrow: 'Admin Permissions' } },
        { path: 'admin/menus', component: AdminMenusPage, meta: { title: '菜单管理', eyebrow: 'Admin Menus' } }
      ]
    }
  ]
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
