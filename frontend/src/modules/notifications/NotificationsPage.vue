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
        :page-sizes="PAGE_DEFAULTS.sizes"
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
import { PAGE_DEFAULTS } from '../../shared/constants'

const notifications = ref([])
const pagination = reactive({ page: PAGE_DEFAULTS.page, size: PAGE_DEFAULTS.size, total: 0 })

/**
 * 分页加载当前用户站内通知，总数同步到分页控件。
 */
async function loadNotifications() {
  try {
    const result = await fetchNotifications(pagination.page, pagination.size)
    notifications.value = result?.records || []
    pagination.total = Number(result?.total || 0)
  } catch (error) {
    ElMessage.error(error.message || '通知加载失败')
  }
}

/**
 * 切换通知页码并保留当前每页数量。
 */
function handlePageChange(page) {
  pagination.page = page
  loadNotifications()
}

/**
 * 每页数量变化后重置到第一页，避免请求越界页。
 */
function handleSizeChange(size) {
  pagination.size = size
  pagination.page = PAGE_DEFAULTS.page
  loadNotifications()
}

/**
 * 将单条未读通知标记为已读，成功后刷新当前页。
 */
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

