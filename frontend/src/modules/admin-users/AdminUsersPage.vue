<!--
 * @Author: AI Auto Code
 * @Date: 2026-06-01
 * @Description: 用户管理页面
-->
<template>
  <section class="page-section">
    <el-card shadow="never">
      <div class="toolbar compact-toolbar">
        <div>
          <p class="eyebrow">Admin Users</p>
          <h2>用户管理</h2>
        </div>
        <div class="header-actions">
          <el-input v-model="keyword" clearable placeholder="搜索账号/昵称" @keyup.enter="loadUsers" />
          <el-button :icon="Search" @click="loadUsers">搜索</el-button>
          <el-button type="primary" @click="openCreate">新建用户</el-button>
        </div>
      </div>
      <el-table :data="users" border>
        <el-table-column prop="id" label="ID" width="80" />
        <el-table-column prop="username" label="账号" />
        <el-table-column prop="nickname" label="昵称" />
        <el-table-column label="状态" width="100">
          <template #default="{ row }">{{ row.status === 1 ? '启用' : '禁用' }}</template>
        </el-table-column>
        <el-table-column label="角色">
          <template #default="{ row }">
            <el-tag v-for="role in row.roles" :key="role">{{ role }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="首次改密" width="110">
          <template #default="{ row }">{{ row.forcePasswordChange ? '是' : '否' }}</template>
        </el-table-column>
        <el-table-column label="操作" width="320">
          <template #default="{ row }">
            <el-button link type="primary" @click="openEdit(row)">编辑</el-button>
            <el-button link type="warning" @click="kick(row)">踢下线</el-button>
            <el-button link type="warning" @click="openReset(row)">重置密码</el-button>
            <el-button link type="danger" @click="disable(row)">禁用</el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <el-dialog v-model="editVisible" :title="editingUser?.id ? '编辑用户' : '新建用户'" width="520px">
      <el-form
        ref="userFormRef"
        label-position="top"
        :model="userForm"
        :rules="userRules"
      >
        <el-form-item label="账号" prop="username">
          <el-input v-model="userForm.username" :disabled="Boolean(editingUser?.id)" />
        </el-form-item>
        <el-form-item v-if="!editingUser?.id" label="初始密码" prop="password">
          <el-input v-model="userForm.password" show-password type="password" />
        </el-form-item>
        <el-form-item label="昵称" prop="nickname">
          <el-input v-model="userForm.nickname" />
        </el-form-item>
        <el-form-item v-if="editingUser?.id" label="状态">
          <el-select v-model="userForm.status">
            <el-option label="启用" :value="1" />
            <el-option label="禁用" :value="0" />
          </el-select>
        </el-form-item>
        <el-form-item label="角色">
          <el-select v-model="userForm.roles" multiple>
            <el-option v-for="role in roles" :key="role.roleCode" :label="role.roleName" :value="role.roleCode" />
          </el-select>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="editVisible = false">取消</el-button>
        <el-button type="primary" @click="saveUser">保存</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="resetVisible" title="重置密码" width="420px">
      <el-form
        ref="resetFormRef"
        label-position="top"
        :model="resetForm"
        :rules="resetRules"
      >
        <el-form-item label="新密码" prop="password">
          <el-input v-model="resetForm.password" show-password type="password" placeholder="请输入新密码" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="resetVisible = false">取消</el-button>
        <el-button type="primary" @click="saveReset">保存</el-button>
      </template>
    </el-dialog>
  </section>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import { ElButton, ElCard, ElDialog, ElForm, ElFormItem, ElInput, ElMessage, ElMessageBox, ElOption, ElSelect, ElTable, ElTableColumn, ElTag } from 'element-plus'
import { Search } from '@element-plus/icons-vue'
import { createUser, disableUser, kickUser, listUsers, resetPassword, updateUser } from './api'
import { listRoles } from '../admin-roles/api'

const keyword = ref('')
const users = ref([])
const roles = ref([])
const editVisible = ref(false)
const resetVisible = ref(false)
const editingUser = ref(null)
const resetUser = ref(null)
const userFormRef = ref(null)
const resetFormRef = ref(null)
const userForm = reactive({ username: '', password: '', nickname: '', status: 1, roles: [] })
const resetForm = reactive({ password: '' })
const userRules = {
  username: [{ required: true, message: '请输入账号', trigger: 'blur' }],
  password: [
    {
      validator: (rule, value, callback) => {
        if (!editingUser.value?.id && !value) {
          callback(new Error('请输入初始密码'))
          return
        }
        if (value && value.length < 8) {
          callback(new Error('密码至少 8 位'))
          return
        }
        callback()
      },
      trigger: 'blur'
    }
  ],
  nickname: [{ required: true, message: '请输入昵称', trigger: 'blur' }]
}
const resetRules = {
  password: [
    { required: true, message: '请输入新密码', trigger: 'blur' },
    { min: 8, message: '新密码至少 8 位', trigger: 'blur' }
  ]
}

async function loadUsers() {
  try {
    const result = await listUsers(keyword.value)
    users.value = result?.records || []
  } catch (error) {
    ElMessage.error(error.message || '用户列表加载失败')
  }
}

async function loadRoles() {
  try {
    roles.value = await listRoles()
  } catch (error) {
    ElMessage.error(error.message || '角色列表加载失败')
  }
}

function openCreate() {
  editingUser.value = null
  Object.assign(userForm, { username: '', password: '', nickname: '', status: 1, roles: [] })
  editVisible.value = true
  userFormRef.value?.clearValidate()
}

function openEdit(row) {
  editingUser.value = row
  Object.assign(userForm, { username: row.username, password: '', nickname: row.nickname, status: row.status, roles: [...(row.roles || [])] })
  editVisible.value = true
  userFormRef.value?.clearValidate()
}

async function saveUser() {
  await userFormRef.value?.validate()
  try {
    if (editingUser.value?.id) {
      await updateUser(editingUser.value.id, { nickname: userForm.nickname, status: userForm.status, roles: userForm.roles })
    } else {
      await createUser({ username: userForm.username, password: userForm.password, nickname: userForm.nickname, roles: userForm.roles })
    }
    ElMessage.success('用户已保存')
    editVisible.value = false
    await loadUsers()
  } catch (error) {
    ElMessage.error(error.message || '用户保存失败')
  }
}

async function kick(row) {
  try {
    await kickUser(row.id)
    ElMessage.success('已踢下线')
  } catch (error) {
    ElMessage.error(error.message || '踢下线失败')
  }
}

function openReset(row) {
  resetUser.value = row
  resetForm.password = ''
  resetVisible.value = true
}

async function saveReset() {
  await resetFormRef.value?.validate()
  try {
    await resetPassword(resetUser.value.id, resetForm.password)
    ElMessage.success('密码已重置')
    resetVisible.value = false
    await loadUsers()
  } catch (error) {
    ElMessage.error(error.message || '密码重置失败')
  }
}

async function disable(row) {
  try {
    await ElMessageBox.confirm(`确认禁用用户 ${row.username}？`, '确认操作')
    await disableUser(row.id)
    ElMessage.success('用户已禁用')
    await loadUsers()
  } catch (error) {
    if (error !== 'cancel') ElMessage.error(error.message || '用户禁用失败')
  }
}

onMounted(async () => {
  await loadRoles()
  await loadUsers()
})
</script>

