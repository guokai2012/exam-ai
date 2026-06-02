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
        <div class="toolbar-actions">
          <el-button :loading="scanning" @click="handleScanMenus">扫描菜单</el-button>
          <el-button type="primary" @click="openCreate()">新建菜单</el-button>
        </div>
      </div>
      <el-table :data="menus" row-key="id" border default-expand-all>
        <el-table-column label="操作" width="220" fixed="left">
          <template #default="{ row }">
            <el-button link type="primary" @click="openCreate(row)">新增子菜单</el-button>
            <el-button link type="primary" @click="openEdit(row)">编辑</el-button>
            <el-button link type="danger" @click="remove(row)">删除</el-button>
          </template>
        </el-table-column>
        <el-table-column prop="menuName" label="菜单名称" min-width="160" />
        <el-table-column label="页面路径" min-width="180">
          <template #default="{ row }">
            <el-tag v-if="!row.path" type="info">分组</el-tag>
            <span v-else>{{ row.path }}</span>
          </template>
        </el-table-column>
        <el-table-column label="API 路径" min-width="220">
          <template #default="{ row }">{{ row.apiPath || '-' }}</template>
        </el-table-column>
        <el-table-column prop="permissionCode" label="权限码" min-width="180" />
        <el-table-column prop="icon" label="图标" width="120" />
        <el-table-column prop="sortOrder" label="排序" width="90" />
        <el-table-column label="状态" width="90">
          <template #default="{ row }">{{ row.status === USER_STATUS.enabled ? '启用' : '禁用' }}</template>
        </el-table-column>
      </el-table>
    </el-card>
    <el-dialog v-model="visible" :title="form.id ? '编辑菜单' : '新建菜单'" width="620px" append-to-body>
      <el-form ref="formRef" :model="form" :rules="formRules" label-position="top">
        <el-form-item v-if="!form.id" label="父级菜单">
          <el-select v-model="form.parentId" filterable clearable placeholder="根菜单留空">
            <el-option
              v-for="option in parentOptions"
              :key="option.value"
              :label="option.label"
              :value="option.value"
            />
          </el-select>
        </el-form-item>
        <el-form-item v-if="!form.id" label="菜单类型">
          <el-checkbox v-model="form.group">分组菜单</el-checkbox>
        </el-form-item>
        <el-form-item v-if="form.id" label="页面路径">
          <el-input :model-value="form.path || '分组'" disabled />
        </el-form-item>
        <el-form-item v-else-if="!form.group" label="页面路径" prop="path">
          <el-input v-model="form.path" placeholder="/example" />
        </el-form-item>
        <el-form-item label="菜单名称" prop="menuName">
          <el-input v-model="form.menuName" />
        </el-form-item>
        <el-form-item label="图标">
          <el-input v-model="form.icon" />
        </el-form-item>
        <el-form-item label="排序" prop="sortOrder">
          <el-input-number v-model="form.sortOrder" :min="0" controls-position="right" />
        </el-form-item>
        <el-form-item label="状态" prop="status">
          <el-select v-model="form.status">
            <el-option label="启用" :value="USER_STATUS.enabled" />
            <el-option label="禁用" :value="USER_STATUS.disabled" />
          </el-select>
        </el-form-item>
        <el-form-item v-if="!form.group && (form.path || form.id)" label="API 路径">
          <el-select v-model="form.apiPath" filterable clearable placeholder="选择 Controller API 根路径">
            <el-option
              v-for="option in apiPathOptions"
              :key="option.value"
              :label="`${option.label}：${option.value}`"
              :value="option.value"
            />
          </el-select>
        </el-form-item>
        <el-form-item v-if="!form.id && !form.group" label="权限码">
          <el-input v-model="form.permissionCode" placeholder="无权限限制可留空" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="visible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="save">保存</el-button>
      </template>
    </el-dialog>
  </section>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import {
  ElButton,
  ElCard,
  ElCheckbox,
  ElDialog,
  ElForm,
  ElFormItem,
  ElInput,
  ElInputNumber,
  ElMessage,
  ElMessageBox,
  ElOption,
  ElSelect,
  ElTable,
  ElTableColumn,
  ElTag
} from 'element-plus'
import { createMenu, deleteMenu, listApiPathOptions, listMenus, requestMenuScanToken, syncScannedMenus, updateMenu } from './api'
import { scanRouterMenus } from './menuScanner'
import { USER_STATUS } from '../../shared/constants'

const menus = ref([])
const apiPathOptions = ref([])
const visible = ref(false)
const saving = ref(false)
const scanning = ref(false)
const formRef = ref(null)
const parentOptions = ref([])
const form = reactive({
  id: null,
  parentId: null,
  group: false,
  menuName: '',
  path: null,
  apiPath: null,
  icon: '',
  sortOrder: 0,
  status: USER_STATUS.enabled,
  permissionCode: ''
})
const formRules = {
  menuName: [{ required: true, message: '请输入菜单名称', trigger: 'blur' }],
  path: [{ validator: validatePath, trigger: 'blur' }],
  sortOrder: [{ required: true, message: '请输入排序值', trigger: 'change' }],
  status: [{ required: true, message: '请选择状态', trigger: 'change' }]
}

/**
 * 加载后台完整菜单树，管理员只能在该树上编辑有限展示字段。
 */
async function load() {
  try {
    menus.value = await listMenus()
    parentOptions.value = buildParentOptions(menus.value)
  } catch (error) {
    ElMessage.error(error.message || '菜单树加载失败')
  }
}

/**
 * 加载可绑定 API 根路径，选项由后端扫描 Controller 映射和 @Tag 名称生成。
 */
async function loadApiPathOptions() {
  try {
    apiPathOptions.value = await listApiPathOptions()
  } catch (error) {
    ElMessage.error(error.message || 'API 路径选项加载失败')
  }
}

/**
 * 打开新建菜单弹窗；从某行进入时自动带入父菜单 ID。
 *
 * @param {Object} parent 父菜单行，可为空。
 */
function openCreate(parent) {
  Object.assign(form, {
    id: null,
    parentId: parent?.id || null,
    group: false,
    menuName: '',
    path: '',
    apiPath: null,
    icon: '',
    sortOrder: 0,
    status: USER_STATUS.enabled,
    permissionCode: ''
  })
  visible.value = true
}

/**
 * 打开编辑弹窗，将表格行复制为表单草稿，避免直接污染菜单树数据。
 *
 * @param {Object} row 当前编辑的菜单行。
 */
function openEdit(row) {
  Object.assign(form, {
    id: row.id,
    parentId: row.parentId || null,
    group: !row.path,
    menuName: row.menuName,
    path: row.path || null,
    apiPath: row.path ? row.apiPath || null : null,
    icon: row.icon || '',
    sortOrder: Number(row.sortOrder || 0),
    status: row.status,
    permissionCode: row.permissionCode || ''
  })
  visible.value = true
}

/**
 * 保存菜单；新增时可维护结构字段，编辑时后端仅更新展示字段和叶子 API 路径。
 */
async function save() {
  await formRef.value?.validate()
  saving.value = true
  try {
    const payload = {
      parentId: form.id ? null : form.parentId || null,
      menuName: form.menuName,
      path: form.group ? null : form.path || null,
      icon: form.icon || null,
      sortOrder: Number(form.sortOrder || 0),
      status: form.status,
      apiPath: form.group ? null : form.apiPath || null,
      permissionCode: form.group ? null : form.permissionCode || null
    }
    if (form.id) await updateMenu(form.id, payload)
    else await createMenu(payload)
    ElMessage.success('菜单已保存')
    visible.value = false
    await load()
  } catch (error) {
    ElMessage.error(error.message || '菜单保存失败')
  } finally {
    saving.value = false
  }
}

/**
 * 删除菜单前进行确认；后端会继续拒绝删除仍有子菜单的节点。
 *
 * @param {Object} row 当前删除的菜单行。
 */
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

/**
 * 扫描当前前端路由菜单元数据，并使用后端短时 Token 同步到菜单表。
 */
async function handleScanMenus() {
  try {
    await ElMessageBox.confirm('扫描会补齐前端路由中声明但菜单表缺失的数据，不会删除已有菜单。确认继续？', '扫描菜单')
    scanning.value = true
    const tokenResponse = await requestMenuScanToken()
    const payload = scanRouterMenus()
    const result = await syncScannedMenus(payload, tokenResponse.token)
    ElMessage.success(`菜单扫描完成，新增 ${result.created}，更新 ${result.updated}，跳过 ${result.skipped}`)
    await load()
  } catch (error) {
    if (error !== 'cancel') {
      ElMessage.error(error.message || '菜单扫描失败')
    }
  } finally {
    scanning.value = false
  }
}

/**
 * 构造父菜单下拉选项，保留层级缩进以便管理员选择新增位置。
 *
 * @param {Array} items 菜单树。
 * @param {number} level 当前层级。
 * @returns {Array} 父菜单选项。
 */
function buildParentOptions(items, level = 0) {
  return items.flatMap(item => [
    { label: `${'　'.repeat(level)}${item.menuName}`, value: item.id },
    ...buildParentOptions(item.children || [], level + 1)
  ])
}

/**
 * 校验叶子菜单必须填写页面路径；分组菜单通过 path 为空表达。
 *
 * @param {Object} rule Element Plus 校验规则。
 * @param {string} value 页面路径。
 * @param {Function} callback 校验回调。
 */
function validatePath(rule, value, callback) {
  if (!form.group && !value) {
    callback(new Error('请输入页面路径'))
    return
  }
  callback()
}

onMounted(async () => {
  await Promise.all([load(), loadApiPathOptions()])
})
</script>

<style scoped>
.toolbar-actions {
  display: flex;
  gap: 8px;
  align-items: center;
}
</style>
