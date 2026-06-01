<!--
 * @Author: AI Auto Code
 * @Date: 2026-06-01
 * @Description: 站内通知页面
-->
<template>
  <section class="page-section">
    <el-card class="admin-panel" shadow="never">
      <div class="panel-head">
        <div>
          <p class="eyebrow">Notifications</p>
          <h2>站内通知</h2>
        </div>
        <el-button :icon="Refresh" @click="loadNotifications">刷新</el-button>
      </div>
      <el-card v-for="notification in notifications" :key="notification.id" class="notification-item" shadow="never">
        <div class="question-title">
          <span>{{ notification.title }}</span>
          <el-tag :type="notification.read ? 'info' : 'warning'">{{ notification.read ? '已读' : '未读' }}</el-tag>
        </div>
        <p>{{ notification.content }}</p>
        <el-button v-if="!notification.read" plain @click="readNotification(notification)">标记已读</el-button>
      </el-card>
      <el-empty v-if="notifications.length === 0" description="暂无系统通知" />
    </el-card>
  </section>
</template>

<script setup>
import { onMounted, ref } from 'vue'
import { ElButton, ElCard, ElEmpty, ElMessage, ElTag } from 'element-plus'
import { Refresh } from '@element-plus/icons-vue'
import { listNotifications as fetchNotifications, markNotificationRead } from './api'

const notifications = ref([])

async function loadNotifications() {
  try {
    const result = await fetchNotifications()
    notifications.value = result?.records || []
  } catch (error) {
    ElMessage.error(error.message || '通知加载失败')
  }
}

async function readNotification(notification) {
  try {
    await markNotificationRead(notification.id)
    ElMessage.success('通知已标记为已读')
    await loadNotifications()
  } catch (error) {
    ElMessage.error(error.message || '通知更新失败')
  }
}

onMounted(loadNotifications)
</script>

