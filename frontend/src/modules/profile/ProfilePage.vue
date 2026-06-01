<!--
 * @Author: AI Auto Code
 * @Date: 2026-06-01
 * @Description: 个人资料页面
-->
<template>
  <section class="page-section">
    <el-card class="profile-card" shadow="never">
      <template #header>
        <strong>当前用户</strong>
      </template>
      <el-descriptions :column="1" border>
        <el-descriptions-item label="用户 ID">{{ profile.userId }}</el-descriptions-item>
        <el-descriptions-item label="账号">{{ profile.username }}</el-descriptions-item>
        <el-descriptions-item label="昵称">{{ profile.nickname || '-' }}</el-descriptions-item>
        <el-descriptions-item label="角色">{{ roleSummary(profile.roles || []) }}</el-descriptions-item>
        <el-descriptions-item label="权限">
          <div class="tag-line">
            <el-tag v-for="permission in profile.permissions || []" :key="permission" type="info">{{ permission }}</el-tag>
          </div>
        </el-descriptions-item>
      </el-descriptions>
    </el-card>
  </section>
</template>

<script setup>
import { onMounted, ref } from 'vue'
import { ElCard, ElDescriptions, ElDescriptionsItem, ElMessage, ElTag } from 'element-plus'
import { currentUser } from '../auth/api'
import { roleSummary } from '../../shared/formatters'

const profile = ref({ roles: [], permissions: [] })

async function loadProfile() {
  try {
    profile.value = await currentUser()
  } catch (error) {
    ElMessage.error(error.message || '用户详情加载失败')
  }
}

onMounted(loadProfile)
</script>

