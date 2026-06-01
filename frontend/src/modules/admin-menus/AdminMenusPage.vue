<!--
 * @Author: AI Auto Code
 * @Date: 2026-06-01
 * @Description: 菜单管理页面
-->
<template>
  <section class="page-section">
    <el-card shadow="never">
      <div class="toolbar compact-toolbar">
        <div><p class="eyebrow">Admin Menus</p><h2>菜单管理</h2></div>
        <el-button type="primary" @click="openCreate()">新建菜单</el-button>
      </div>
      <el-table :data="menus" row-key="id" border default-expand-all>
        <el-table-column prop="menuName" label="菜单名称" />
        <el-table-column prop="path" label="路径" />
        <el-table-column prop="component" label="组件标识" />
        <el-table-column prop="permissionCode" label="权限码" />
        <el-table-column prop="sortOrder" label="排序" width="90" />
        <el-table-column label="状态" width="90">
          <template #default="{ row }">{{ row.status === 1 ? '启用' : '禁用' }}</template>
        </el-table-column>
        <el-table-column label="操作" width="220">
          <template #default="{ row }">
            <el-button link type="primary" @click="openCreate(row)">新增子菜单</el-button>
            <el-button link type="primary" @click="openEdit(row)">编辑</el-button>
            <el-button link type="danger" @click="remove(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>
    <el-dialog v-model="visible" :title="form.id ? '编辑菜单' : '新建菜单'" width="620px">
      <el-form ref="formRef" :model="form" :rules="formRules" label-position="top">
        <el-form-item label="父级 ID"><el-input v-model="form.parentId" placeholder="根菜单留空" /></el-form-item>
        <el-form-item label="菜单名称" prop="menuName"><el-input v-model="form.menuName" /></el-form-item>
        <el-form-item label="路径" prop="path"><el-input v-model="form.path" /></el-form-item>
        <el-form-item label="组件标识" prop="component">
          <el-select v-model="form.component" filterable placeholder="选择已存在的前端组件">
            <el-option v-for="option in componentOptions" :key="option.value" :label="option.label" :value="option.value" />
          </el-select>
        </el-form-item>
        <el-form-item label="图标"><el-input v-model="form.icon" /></el-form-item>
        <el-form-item label="权限码"><el-input v-model="form.permissionCode" placeholder="无权限限制可留空" /></el-form-item>
        <el-form-item label="排序"><el-input v-model.number="form.sortOrder" /></el-form-item>
        <el-form-item label="状态">
          <el-select v-model="form.status">
            <el-option label="启用" :value="1" />
            <el-option label="禁用" :value="0" />
          </el-select>
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
import { onMounted, reactive, ref } from 'vue'
import { ElButton, ElCard, ElDialog, ElForm, ElFormItem, ElInput, ElMessage, ElMessageBox, ElOption, ElSelect, ElTable, ElTableColumn } from 'element-plus'
import { createMenu, deleteMenu, listMenus, updateMenu } from './api'

const menus = ref([])
const visible = ref(false)
const formRef = ref(null)
const form = reactive({ id: null, parentId: '', menuName: '', path: '', component: '', icon: '', permissionCode: '', sortOrder: 0, status: 1 })
const componentOptions = [
  { label: '菜单分组', value: 'MenuGroup' },
  { label: '我的文档', value: 'DocumentsPage' },
  { label: '可用题', value: 'AvailableQuestionsPage' },
  { label: '待确认题', value: 'PendingConfirmQuestionsPage' },
  { label: '站内通知', value: 'NotificationsPage' },
  { label: '用户详情', value: 'ProfilePage' },
  { label: '系统配置', value: 'SystemConfigPage' },
  { label: '用户管理', value: 'AdminUsersPage' },
  { label: '角色管理', value: 'AdminRolesPage' },
  { label: '权限管理', value: 'AdminPermissionsPage' },
  { label: '菜单管理', value: 'AdminMenusPage' }
]
const formRules = {
  menuName: [{ required: true, message: '请输入菜单名称', trigger: 'blur' }],
  path: [{ required: true, message: '请输入路径', trigger: 'blur' }],
  component: [{ required: true, message: '请输入组件标识', trigger: 'blur' }]
}

async function load() {
  try {
    menus.value = await listMenus()
  } catch (error) {
    ElMessage.error(error.message || '菜单树加载失败')
  }
}
function openCreate(parent) {
  Object.assign(form, { id: null, parentId: parent?.id || '', menuName: '', path: '', component: '', icon: '', permissionCode: '', sortOrder: 0, status: 1 })
  visible.value = true
}
function openEdit(row) {
  Object.assign(form, { ...row, parentId: row.parentId || '', permissionCode: row.permissionCode || '', icon: row.icon || '' })
  visible.value = true
}
async function save() {
  await formRef.value?.validate()
  if (!componentOptions.some(option => option.value === form.component)) {
    ElMessage.error('请选择当前前端已存在的组件标识')
    return
  }
  try {
    const payload = {
      parentId: form.parentId ? Number(form.parentId) : null,
      menuName: form.menuName,
      path: form.path,
      component: form.component,
      icon: form.icon,
      permissionCode: form.permissionCode || null,
      sortOrder: Number(form.sortOrder || 0),
      status: form.status
    }
    if (form.id) await updateMenu(form.id, payload)
    else await createMenu(payload)
    ElMessage.success('菜单已保存')
    visible.value = false
    await load()
  } catch (error) {
    ElMessage.error(error.message || '菜单保存失败')
  }
}
async function remove(row) {
  try {
    await ElMessageBox.confirm(`确认删除菜单 ${row.menuName}？`, '确认操作')
    await deleteMenu(row.id)
    ElMessage.success('菜单已删除')
    await load()
  } catch (error) {
    if (error !== 'cancel') ElMessage.error(error.message || '菜单删除失败')
  }
}
onMounted(load)
</script>
