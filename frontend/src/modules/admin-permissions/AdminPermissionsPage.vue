<!--
 * @Author: AI Auto Code
 * @Date: 2026-06-02
 * @Description: Controller 扫描权限只读管理页面
-->
<template>
  <section class="page-section">
    <el-card shadow="never">
      <div class="toolbar compact-toolbar">
        <div><p class="eyebrow">Admin Permissions</p><h2>权限管理</h2></div>
        <div class="header-actions">
          <el-button type="primary" :loading="scanning" @click="handleScan">扫描权限</el-button>
        </div>
      </div>
      <el-table :data="permissions" row-key="id" border default-expand-all>
        <el-table-column prop="permissionName" label="权限名称" min-width="240" />
        <el-table-column prop="permissionCode" label="权限码" min-width="240">
          <template #default="{ row }">
            <span v-if="!row.permissionCode?.startsWith('__')">{{ row.permissionCode }}</span>
            <span v-else class="muted-text">系统分组</span>
          </template>
        </el-table-column>
        <el-table-column label="类型" width="110">
          <template #default="{ row }">
            <el-tag :type="typeTag(row.permissionType)">{{ typeLabel(row.permissionType) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="来源" width="130">
          <template #default>Controller 扫描</template>
        </el-table-column>
      </el-table>
    </el-card>
  </section>
</template>

<script setup>
import { onMounted, ref } from 'vue'
import { ElButton, ElCard, ElMessage, ElTable, ElTableColumn, ElTag } from 'element-plus'
import { listPermissions, scanPermissions } from './api'

const permissions = ref([])
const scanning = ref(false)

async function loadPermissions() {
  try {
    // 权限数据只读展示，所有变更必须来自后端 Controller 扫描。
    permissions.value = await listPermissions()
  } catch (error) {
    ElMessage.error(error.message || '权限树加载失败')
  }
}

async function handleScan() {
  scanning.value = true
  try {
    // 扫描成功后刷新树，确保管理员看到的是最新的全量同步结果。
    const result = await scanPermissions()
    ElMessage.success(`扫描完成：新增 ${result.created}，更新 ${result.updated}，删除 ${result.deleted}`)
    await loadPermissions()
  } catch (error) {
    ElMessage.error(error.message || '权限扫描失败')
  } finally {
    scanning.value = false
  }
}

function typeLabel(type) {
  return {
    GROUP: '分组',
    ACTION: '动作'
  }[type] || type
}

function typeTag(type) {
  return {
    GROUP: 'info',
    ACTION: 'warning'
  }[type] || 'info'
}

onMounted(loadPermissions)
</script>
