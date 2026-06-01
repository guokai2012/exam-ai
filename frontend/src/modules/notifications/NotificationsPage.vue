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
      <el-pagination
        class="table-pagination"
        background
        layout="total, sizes, prev, pager, next"
        :current-page="pagination.page"
        :page-size="pagination.size"
        :page-sizes="[10, 20, 50]"
        :total="pagination.total"
        @current-change="handlePageChange"
        @size-change="handleSizeChange"
      />
    </el-card>
  </section>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import { ElButton, ElCard, ElEmpty, ElMessage, ElPagination, ElTag } from 'element-plus'
import { Refresh } from '@element-plus/icons-vue'
import { listNotifications as fetchNotifications, markNotificationRead } from './api'

const notifications = ref([])
const pagination = reactive({ page: 1, size: 20, total: 0 })

async function loadNotifications() {
  try {
    const result = await fetchNotifications(pagination.page, pagination.size)
    notifications.value = result?.records || []
    pagination.total = Number(result?.total || 0)
  } catch (error) {
    ElMessage.error(error.message || '通知加载失败')
  }
}

function handlePageChange(page) {
  pagination.page = page
  loadNotifications()
}

function handleSizeChange(size) {
  pagination.size = size
  pagination.page = 1
  loadNotifications()
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

