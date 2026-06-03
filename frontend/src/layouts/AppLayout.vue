<!--
 * @Author: AI Auto Code
 * @Date: 2026-06-01
 * @Description: 后台应用布局
-->
<template>
  <el-container class="admin-layout">
    <el-aside class="admin-sidebar" width="260px">
      <div class="sidebar-brand">
        <p class="eyebrow">Exam AI</p>
        <h1>智能出题系统</h1>
      </div>
      <button class="user-card" type="button" @click="router.push('/profile')">
        <el-avatar :size="42">{{ userInitial }}</el-avatar>
        <span>
          <strong>{{ currentUserInfo.nickname || currentUserInfo.username || '未登录' }}</strong>
          <small>{{ roleSummary(currentUserInfo.roles || []) }}</small>
        </span>
      </button>
      <el-menu class="side-menu" :default-active="route.path" :default-openeds="defaultOpeneds" router>
        <SideMenuNode v-for="menu in visibleMenus" :key="menu.path || menu.id" :menu="menu" />
      </el-menu>
    </el-aside>

    <el-container class="admin-main">
      <el-header class="admin-header">
        <div>
          <p class="eyebrow">{{ route.meta.eyebrow || 'Workspace' }}</p>
          <h2>{{ route.meta.title || '工作区' }}</h2>
        </div>
        <div class="header-actions">
          <el-button :icon="Setting" @click="router.push('/system-configs')">系统设置</el-button>
          <el-button :icon="Bell" @click="router.push('/notifications')">站内通知</el-button>
          <el-dropdown trigger="click" @command="handleSettingCommand">
            <el-button :icon="Brush">主题与账号</el-button>
            <template #dropdown>
              <el-dropdown-menu>
                <el-dropdown-item disabled>主题配色：{{ themeLabel(currentTheme) }}</el-dropdown-item>
                <el-dropdown-item command="theme:blue">默认蓝</el-dropdown-item>
                <el-dropdown-item command="theme:green">绿色</el-dropdown-item>
                <el-dropdown-item command="theme:dark">深色</el-dropdown-item>
                <el-dropdown-item divided command="profile">用户详情</el-dropdown-item>
                <el-dropdown-item command="change-password">修改密码</el-dropdown-item>
                <el-dropdown-item command="logout">退出登录</el-dropdown-item>
              </el-dropdown-menu>
            </template>
          </el-dropdown>
        </div>
      </el-header>
      <el-main class="admin-workspace">
        <router-view />
      </el-main>
    </el-container>
  </el-container>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import {
  ElAside,
  ElAvatar,
  ElButton,
  ElContainer,
  ElDropdown,
  ElDropdownItem,
  ElDropdownMenu,
  ElHeader,
  ElMain,
  ElMenu,
  ElMessage
} from 'element-plus'
import { Bell, Brush, Setting } from '@element-plus/icons-vue'
import { currentUser, logout } from '../modules/auth/api'
import { myMenus } from '../modules/admin-menus/api'
import { clearAuthTokens, getRefreshToken } from '../shared/authToken'
import { applyTheme, getTheme, setTheme, themeLabel } from '../modules/settings/themeSettings'
import { roleSummary } from '../shared/formatters'
import { setMenuApiPathSource } from '../shared/menuApiPath'
import SideMenuNode from '../components/SideMenuNode.vue'

const route = useRoute()
const router = useRouter()
const currentUserInfo = ref({})
const currentTheme = ref(getTheme())
const menus = ref([])
const userInitial = computed(() => (currentUserInfo.value.nickname || currentUserInfo.value.username || '用').slice(0, 1).toUpperCase())

async function loadCurrentUser() {
  try {
    currentUserInfo.value = await currentUser()
    if (currentUserInfo.value?.forcePasswordChange && route.path !== '/change-password') {
      await router.replace('/change-password')
    }
  } catch (error) {
    ElMessage.error(error.message || '当前用户信息加载失败')
  }
}

const fallbackMenus = [
  { menuName: '首页', path: '/home', icon: 'House' },
  { menuName: '我的文档', path: '/documents', apiPath: '/api/documents', icon: 'Document' },
  {
    menuName: '题库管理',
    path: null,
    icon: 'Collection',
    children: [
      { menuName: '可用题', path: '/questions/available', apiPath: '/api/questions', icon: 'Collection' },
      { menuName: '待确认题', path: '/questions/pending-confirm', apiPath: '/api/questions', icon: 'EditPen' }
    ]
  },
  { menuName: '站内通知', path: '/notifications', apiPath: '/api/notifications', icon: 'Bell' },
  { menuName: '用户详情', path: '/profile', icon: 'User' }
]

const visibleMenus = computed(() => menus.value.length ? menus.value : fallbackMenus)
const defaultOpeneds = computed(() => groupIndexes(visibleMenus.value))

function groupIndexes(items) {
  return items.flatMap((item) => {
    const children = item.children || []
    if (!children.length) {
      return []
    }
    return [item.path || String(item.id), ...groupIndexes(children)]
  })
}

async function loadMenus() {
  try {
    menus.value = await myMenus()
  } catch {
    menus.value = fallbackMenus
  }
  // 页面 API 模块优先读取当前菜单绑定的 apiPath，未配置时再使用模块默认值。
  setMenuApiPathSource(visibleMenus.value)
}

async function handleSettingCommand(command) {
  if (command.startsWith('theme:')) {
    currentTheme.value = command.split(':')[1]
    setTheme(currentTheme.value)
    return
  }
  if (command === 'profile') {
    await router.push('/profile')
    return
  }
  if (command === 'change-password') {
    await router.push('/change-password')
    return
  }
  if (command === 'logout') {
    try {
      await logout(getRefreshToken())
    } catch {
      // 即使服务端退出失败，也清理本地会话，避免用户卡在当前页面。
    }
    clearAuthTokens()
    ElMessage.success('已退出登录')
    await router.replace('/login')
  }
}

onMounted(async () => {
  applyTheme(currentTheme.value)
  await loadCurrentUser()
  await loadMenus()
})
</script>

