<!--
 * @Author: AI Auto Code
 * @Date: 2026-06-01
 * @Description: 角色管理页面
-->
<template>
  <section class="page-section">
    <el-card shadow="never">
      <div class="toolbar compact-toolbar">
        <div><p class="eyebrow">Admin Roles</p><h2>角色管理</h2></div>
        <el-button type="primary" @click="openCreate">新建角色</el-button>
      </div>
      <el-table :data="roles" border>
        <el-table-column label="操作" width="160" fixed="left">
          <template #default="{ row }">
            <el-button link type="primary" @click="openEdit(row)">编辑</el-button>
            <el-button link type="danger" @click="remove(row)">删除</el-button>
          </template>
        </el-table-column>
        <el-table-column prop="id" label="ID" width="80" />
        <el-table-column prop="roleCode" label="角色编码" />
        <el-table-column prop="roleName" label="角色名称" />
        <el-table-column label="权限">
          <template #default="{ row }">
            <el-tag v-for="permission in row.permissions" :key="permission" type="info">{{ permission }}</el-tag>
          </template>
        </el-table-column>
      </el-table>
    </el-card>
    <el-dialog v-model="visible" :title="form.id ? '编辑角色' : '新建角色'" width="680px">
      <el-form ref="formRef" :model="form" :rules="formRules" label-position="top">
        <el-form-item label="角色编码" prop="roleCode"><el-input v-model="form.roleCode" /></el-form-item>
        <el-form-item label="角色名称" prop="roleName"><el-input v-model="form.roleName" /></el-form-item>
        <el-form-item label="权限">
          <el-tree
            ref="permissionTreeRef"
            class="permission-tree"
            :data="permissions"
            node-key="permissionCode"
            :props="treeProps"
            show-checkbox
            default-expand-all
            check-on-click-node
          >
            <template #default="{ data }">
              <span class="permission-node">
                <span>{{ data.permissionName }}</span>
                <small v-if="data.assignable && !data.permissionCode?.startsWith('__')">{{ data.permissionCode }}</small>
              </span>
            </template>
          </el-tree>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="visible = false">取消</el-button>
        <el-button type="primary" @click="save">保存</el-button>
      </template>
    </el-dialog>
  </section>
</template>

<script setup>
import { nextTick, onMounted, reactive, ref } from 'vue'
import { ElButton, ElCard, ElDialog, ElForm, ElFormItem, ElInput, ElMessage, ElMessageBox, ElTable, ElTableColumn, ElTag, ElTree } from 'element-plus'
import { createRole, deleteRole, listRoles, updateRole } from './api'
import { listPermissions } from '../admin-permissions/api'

const roles = ref([])
const permissions = ref([])
const visible = ref(false)
const permissionTreeRef = ref(null)
const formRef = ref(null)
const treeProps = { label: 'permissionName', children: 'children' }
const form = reactive({ id: null, roleCode: '', roleName: '', permissions: [] })
const formRules = {
  roleCode: [{ required: true, message: '请输入角色编码', trigger: 'blur' }],
  roleName: [{ required: true, message: '请输入角色名称', trigger: 'blur' }]
}

async function load() {
  try {
    roles.value = await listRoles()
    permissions.value = await listPermissions()
  } catch (error) {
    ElMessage.error(error.message || '角色数据加载失败')
  }
}

async function openCreate() {
  Object.assign(form, { id: null, roleCode: '', roleName: '', permissions: [] })
  visible.value = true
  await nextTick()
  permissionTreeRef.value?.setCheckedKeys([])
}

async function openEdit(row) {
  Object.assign(form, { id: row.id, roleCode: row.roleCode, roleName: row.roleName, permissions: [...(row.permissions || [])] })
  visible.value = true
  await nextTick()
  permissionTreeRef.value?.setCheckedKeys(form.permissions)
}

async function save() {
  await formRef.value?.validate()
  try {
    // 仅提交可分配的权限码，菜单/分组节点只承担树形展示和批量勾选。
    const checkedNodes = permissionTreeRef.value?.getCheckedNodes(false, false) || []
    const permissionCodes = checkedNodes
      .filter(permission => permission.assignable && !permission.permissionCode?.startsWith('__'))
      .map(permission => permission.permissionCode)
    const payload = { roleCode: form.roleCode, roleName: form.roleName, permissions: [...new Set(permissionCodes)] }
    if (form.id) await updateRole(form.id, payload)
    else await createRole(payload)
    ElMessage.success('角色已保存')
    visible.value = false
    await load()
  } catch (error) {
    ElMessage.error(error.message || '角色保存失败')
  }
}

async function remove(row) {
  try {
    await ElMessageBox.confirm(`确认删除角色 ${row.roleCode}？`, '确认操作')
    await deleteRole(row.id)
    ElMessage.success('角色已删除')
    await load()
  } catch (error) {
    if (error !== 'cancel') ElMessage.error(error.message || '角色删除失败')
  }
}

onMounted(load)
</script>

<style scoped>
.permission-tree {
  width: 100%;
  max-height: 360px;
  overflow: auto;
  border: 1px solid var(--el-border-color);
  border-radius: 6px;
  padding: 8px;
}

.permission-node {
  display: inline-flex;
  align-items: center;
  gap: 10px;
  min-width: 0;
}

.permission-node small {
  color: var(--el-text-color-secondary);
}
</style>
