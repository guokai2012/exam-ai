<!--
 * @Author: AI Auto Code
 * @Date: 2026-06-01
 * @Description: 侧边菜单节点组件
-->
<template>
  <el-sub-menu v-if="hasChildren" :index="menu.path || String(menu.id)">
    <template #title>
      <el-icon><component :is="iconComponent(menu.icon)" /></el-icon>
      <span>{{ menu.menuName }}</span>
    </template>
    <SideMenuNode v-for="child in menu.children" :key="child.path || child.id" :menu="child" />
  </el-sub-menu>
  <el-menu-item v-else :index="menu.path">
    <el-icon><component :is="iconComponent(menu.icon)" /></el-icon>
    <span>{{ menu.menuName }}</span>
  </el-menu-item>
</template>

<script setup>
import { computed } from 'vue'
import { ElIcon, ElMenuItem, ElSubMenu } from 'element-plus'
import { Bell, Collection, Document, EditPen, Key, Menu, Setting, User, UserFilled } from '@element-plus/icons-vue'

defineOptions({ name: 'SideMenuNode' })

const props = defineProps({
  menu: {
    type: Object,
    required: true
  }
})

const hasChildren = computed(() => Array.isArray(props.menu.children) && props.menu.children.length > 0)
const iconMap = { Bell, Collection, Document, EditPen, Key, Menu, Setting, User, UserFilled }

function iconComponent(icon) {
  return iconMap[icon] || Document
}
</script>

