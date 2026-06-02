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
          <el-button :icon="Search" @click="handleSearch">搜索</el-button>
          <el-button type="primary" @click="openCreate">新建用户</el-button>
        </div>
      </div>
      <el-table :data="users" border>
        <el-table-column label="操作" width="230" fixed="left">
          <template #default="{ row }">
            <el-button link type="primary" @click="openEdit(row)">编辑</el-button>
            <el-button link type="warning" @click="kick(row)">踢下线</el-button>
            <el-dropdown @command="command => handleMoreCommand(command, row)">
              <el-button link type="primary">更多</el-button>
              <template #dropdown>
                <el-dropdown-menu>
                  <el-dropdown-item command="reset">重置密码</el-dropdown-item>
                  <el-dropdown-item command="disable">禁用</el-dropdown-item>
                </el-dropdown-menu>
              </template>
            </el-dropdown>
          </template>
        </el-table-column>
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
      </el-table>
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
import {
  ElButton,
  ElCard,
  ElDialog,
  ElDropdown,
  ElDropdownItem,
  ElDropdownMenu,
  ElForm,
  ElFormItem,
  ElInput,
  ElMessage,
  ElMessageBox,
  ElOption,
  ElPagination,
  ElSelect,
  ElTable,
  ElTableColumn,
  ElTag
} from 'element-plus'
import { Search } from '@element-plus/icons-vue'
import { createUser, disableUser, kickUser, listUsers, resetPassword, updateUser } from './api'
import { listRoles } from '../admin-roles/api'
import { PAGE_DEFAULTS, USER_STATUS, VALIDATION_LIMITS } from '../../shared/constants'

const keyword = ref('')
const users = ref([])
const roles = ref([])
const pagination = reactive({ page: PAGE_DEFAULTS.page, size: PAGE_DEFAULTS.size, total: 0 })
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
        if (value && value.length < VALIDATION_LIMITS.passwordStrongMin) {
          callback(new Error(`密码至少 ${VALIDATION_LIMITS.passwordStrongMin} 位`))
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
    { min: VALIDATION_LIMITS.passwordStrongMin, message: `新密码至少 ${VALIDATION_LIMITS.passwordStrongMin} 位`, trigger: 'blur' }
  ]
}

/**
 * 按关键字和分页参数加载用户列表，查询结果总数驱动分页组件。
 */
async function loadUsers() {
  try {
    const result = await listUsers(keyword.value, pagination.page, pagination.size)
    users.value = result?.records || []
    pagination.total = Number(result?.total || 0)
  } catch (error) {
    ElMessage.error(error.message || '用户列表加载失败')
  }
}

/**
 * 搜索条件变化后从第一页开始查询，避免沿用旧页码导致结果为空。
 */
function handleSearch() {
  pagination.page = PAGE_DEFAULTS.page
  loadUsers()
}

/**
 * 切换用户列表页码并保留当前关键字条件。
 */
function handlePageChange(page) {
  pagination.page = page
  loadUsers()
}

/**
 * 每页数量变化后重置页码，保证分页边界与后端查询一致。
 */
function handleSizeChange(size) {
  pagination.size = size
  pagination.page = PAGE_DEFAULTS.page
  loadUsers()
}

/**
 * 加载角色列表供新建和编辑用户时分配角色。
 */
async function loadRoles() {
  try {
    roles.value = await listRoles()
  } catch (error) {
    ElMessage.error(error.message || '角色列表加载失败')
  }
}

/**
 * 打开新建用户弹窗，并重置表单状态。
 */
function openCreate() {
  editingUser.value = null
  Object.assign(userForm, { username: '', password: '', nickname: '', status: USER_STATUS.enabled, roles: [] })
  editVisible.value = true
  userFormRef.value?.clearValidate()
}

/**
 * 打开编辑用户弹窗，将当前行数据复制到表单草稿，避免直接修改表格数据。
 */
function openEdit(row) {
  editingUser.value = row
  Object.assign(userForm, { username: row.username, password: '', nickname: row.nickname, status: row.status, roles: [...(row.roles || [])] })
  editVisible.value = true
  userFormRef.value?.clearValidate()
}

/**
 * 根据当前弹窗状态执行新建或编辑用户，成功后刷新列表。
 */
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

/**
 * 撤销指定用户会话，用于管理员处理账号风险或权限变更后强制重新登录。
 */
async function kick(row) {
  try {
    await kickUser(row.id)
    ElMessage.success('已踢下线')
  } catch (error) {
    ElMessage.error(error.message || '踢下线失败')
  }
}

/**
 * 处理用户行更多操作，下拉菜单用于收纳第三个及之后的低频按钮。
 *
 * @param {string} command 操作命令。
 * @param {Object} row 当前用户行。
 */
function handleMoreCommand(command, row) {
  if (command === 'reset') {
    openReset(row)
    return
  }
  if (command === 'disable') {
    disable(row)
  }
}

/**
 * 打开重置密码弹窗，并清理上一次输入的密码草稿。
 */
function openReset(row) {
  resetUser.value = row
  resetForm.password = ''
  resetVisible.value = true
}

/**
 * 保存管理员重置的新密码，后端会要求用户下次登录后修改密码。
 */
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

/**
 * 禁用用户前二次确认，避免误操作导致账号不可用。
 */
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

