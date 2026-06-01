<!--
 * @Author: AI Auto Code
 * @Date: 2026-06-01
 * @Description: 权限管理页面
-->
<template>
  <section class="page-section">
    <el-card shadow="never">
      <div class="toolbar compact-toolbar">
        <div><p class="eyebrow">Admin Permissions</p><h2>权限管理</h2></div>
        <div class="header-actions">
          <el-button @click="openCreate">新建权限</el-button>
          <el-button type="primary" :loading="scanning" @click="scan">扫描权限</el-button>
        </div>
      </div>
      <el-table :data="permissions" row-key="id" border default-expand-all>
        <el-table-column prop="permissionName" label="权限名称" min-width="220" />
        <el-table-column prop="permissionCode" label="权限码" min-width="220">
          <template #default="{ row }">
            <span v-if="!row.permissionCode?.startsWith('__')">{{ row.permissionCode }}</span>
            <span v-else class="muted-text">系统节点</span>
          </template>
        </el-table-column>
        <el-table-column label="类型" width="110">
          <template #default="{ row }">
            <el-tag :type="typeTag(row.permissionType)">{{ typeLabel(row.permissionType) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="来源" width="110">
          <template #default="{ row }">{{ row.systemGenerated ? '系统生成' : '手动维护' }}</template>
        </el-table-column>
        <el-table-column label="操作" width="150">
          <template #default="{ row }">
            <el-button v-if="canEdit(row)" link type="primary" @click="openEdit(row)">编辑</el-button>
            <el-button v-if="canDelete(row)" link type="danger" @click="remove(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>
    <el-dialog v-model="visible" :title="form.id ? '编辑权限' : '新建权限'" width="520px">
      <el-form ref="formRef" :model="form" :rules="formRules" label-position="top">
        <el-form-item label="权限码" prop="permissionCode"><el-input v-model="form.permissionCode" /></el-form-item>
        <el-form-item label="权限名称" prop="permissionName"><el-input v-model="form.permissionName" /></el-form-item>
        <el-form-item label="排序" prop="sortOrder"><el-input v-model.number="form.sortOrder" /></el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="visible = false">取消</el-button>
        <el-button type="primary" @click="save">保存</el-button>
      </template>
    </el-dialog>
  </section>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import { ElButton, ElCard, ElDialog, ElForm, ElFormItem, ElInput, ElMessage, ElMessageBox, ElTable, ElTableColumn, ElTag } from 'element-plus'
import { createPermission, deletePermission, listPermissions, scanPermissions, updatePermission } from './api'

const permissions = ref([])
const visible = ref(false)
const scanning = ref(false)
const formRef = ref(null)
const form = reactive({ id: null, parentId: null, permissionCode: '', permissionName: '', sortOrder: 0 })
const formRules = {
  permissionCode: [{ required: true, message: '请输入权限码', trigger: 'blur' }],
  permissionName: [{ required: true, message: '请输入权限名称', trigger: 'blur' }]
}

function openCreate() {
  Object.assign(form, { id: null, parentId: null, permissionCode: '', permissionName: '', sortOrder: 0 })
  visible.value = true
  formRef.value?.clearValidate()
}

async function load() {
  try {
    permissions.value = await listPermissions()
  } catch (error) {
    ElMessage.error(error.message || '权限树加载失败')
  }
}

function openEdit(row) {
  Object.assign(form, {
    id: row.id,
    parentId: row.parentId,
    permissionCode: row.permissionCode,
    permissionName: row.permissionName,
    sortOrder: row.sortOrder || 0
  })
  visible.value = true
}

async function save() {
  await formRef.value?.validate()
  try {
    const payload = {
      parentId: form.parentId,
      permissionCode: form.permissionCode,
      permissionName: form.permissionName,
      sortOrder: Number(form.sortOrder || 0)
    }
    if (form.id) await updatePermission(form.id, payload)
    else await createPermission(payload)
    ElMessage.success('权限已保存')
    visible.value = false
    await load()
  } catch (error) {
    ElMessage.error(error.message || '权限保存失败')
  }
}

async function remove(row) {
  try {
    await ElMessageBox.confirm(`确认删除权限 ${row.permissionCode}？`, '确认操作')
    await deletePermission(row.id)
    ElMessage.success('权限已删除')
    await load()
  } catch (error) {
    if (error !== 'cancel') ElMessage.error(error.message || '权限删除失败')
  }
}

async function scan() {
  scanning.value = true
  try {
    const result = await scanPermissions()
    ElMessage.success(`扫描完成：新增 ${result.created}，更新 ${result.updated}，删除 ${result.deleted}`)
    await load()
  } catch (error) {
    ElMessage.error(error.message || '权限扫描失败')
  } finally {
    scanning.value = false
  }
}

function canEdit(row) {
  return row.permissionType === 'ACTION'
}

function canDelete(row) {
  return row.permissionType === 'ACTION'
}

function typeLabel(type) {
  return {
    GROUP: '分组',
    MENU: '菜单',
    VIEW: '查看',
    ACTION: '动作'
  }[type] || type
}

function typeTag(type) {
  return {
    GROUP: 'info',
    MENU: 'info',
    VIEW: 'success',
    ACTION: 'warning'
  }[type] || 'info'
}

onMounted(load)
</script>
