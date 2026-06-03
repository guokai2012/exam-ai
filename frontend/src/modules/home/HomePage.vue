<!--
 * @Author: AI Auto Code
 * @Date: 2026-06-03
 * @Description: 登录后首页工作台
-->
<template>
  <section class="page-section home-page">
    <div class="home-grid">
      <el-card class="home-panel" shadow="never">
        <template #header>
          <div class="panel-head">
            <strong>快捷入口</strong>
          </div>
        </template>
        <div class="quick-actions">
          <el-button :icon="Document" type="primary" @click="router.push('/documents')">我的文档</el-button>
          <el-button :icon="Collection" @click="router.push('/questions/available')">可用题</el-button>
          <el-button :icon="Bell" @click="router.push('/notifications')">站内通知</el-button>
        </div>
      </el-card>

      <el-card class="home-panel" shadow="never">
        <template #header>
          <div class="panel-head">
            <strong>当前账号</strong>
          </div>
        </template>
        <el-descriptions :column="1" border>
          <el-descriptions-item label="账号">{{ profile.username || '-' }}</el-descriptions-item>
          <el-descriptions-item label="昵称">{{ profile.nickname || '-' }}</el-descriptions-item>
          <el-descriptions-item label="角色">{{ roleSummary(profile.roles || []) }}</el-descriptions-item>
        </el-descriptions>
      </el-card>
    </div>
  </section>
</template>

<script setup>
import { onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElButton, ElCard, ElDescriptions, ElDescriptionsItem, ElMessage } from 'element-plus'
import { Bell, Collection, Document } from '@element-plus/icons-vue'
import { currentUser } from '../auth/api'
import { roleSummary } from '../../shared/formatters'

const router = useRouter()
const profile = ref({ roles: [] })

/**
 * 加载当前登录用户概要，首页只展示账号入口信息，不承担权限判定逻辑。
 */
async function loadProfile() {
  try {
    profile.value = await currentUser()
  } catch (error) {
    ElMessage.error(error.message || '首页信息加载失败')
  }
}

onMounted(loadProfile)
</script>

<style scoped>
.home-grid {
  display: grid;
  grid-template-columns: minmax(0, 1.2fr) minmax(320px, 0.8fr);
  gap: 18px;
}

.home-panel {
  min-height: 220px;
}

.quick-actions {
  display: flex;
  flex-wrap: wrap;
  gap: 12px;
}

@media (max-width: 960px) {
  .home-grid {
    grid-template-columns: 1fr;
  }
}
</style>
