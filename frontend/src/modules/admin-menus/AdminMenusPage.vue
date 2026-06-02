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
      </div>
      <el-table :data="menus" row-key="id" border default-expand-all>
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
        <el-table-column label="操作" width="90">
          <template #default="{ row }">
            <el-button link type="primary" @click="openEdit(row)">编辑</el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>
    <el-dialog v-model="visible" title="编辑菜单" width="620px" append-to-body>
      <el-form ref="formRef" :model="form" :rules="formRules" label-position="top">
        <el-form-item label="页面路径">
          <el-input :model-value="form.path || '分组'" disabled />
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
        <el-form-item v-if="form.path" label="API 路径">
          <el-select v-model="form.apiPath" filterable clearable placeholder="选择 Controller API 根路径">
            <el-option
              v-for="option in apiPathOptions"
              :key="option.value"
              :label="`${option.label}：${option.value}`"
              :value="option.value"
            />
          </el-select>
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
  ElDialog,
  ElForm,
  ElFormItem,
  ElInput,
  ElInputNumber,
  ElMessage,
  ElOption,
  ElSelect,
  ElTable,
  ElTableColumn,
  ElTag
} from 'element-plus'
import { listApiPathOptions, listMenus, updateMenu } from './api'
import { USER_STATUS } from '../../shared/constants'

const menus = ref([])
const apiPathOptions = ref([])
const visible = ref(false)
const saving = ref(false)
const formRef = ref(null)
const form = reactive({ id: null, menuName: '', path: null, apiPath: null, icon: '', sortOrder: 0, status: USER_STATUS.enabled })
const formRules = {
  menuName: [{ required: true, message: '请输入菜单名称', trigger: 'blur' }],
  sortOrder: [{ required: true, message: '请输入排序值', trigger: 'change' }],
  status: [{ required: true, message: '请选择状态', trigger: 'change' }]
}

/**
 * 加载后台完整菜单树，管理员只能在该树上编辑有限展示字段。
 */
async function load() {
  try {
    menus.value = await listMenus()
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
 * 打开编辑弹窗，将表格行复制为表单草稿，避免直接污染菜单树数据。
 *
 * @param {Object} row 当前编辑的菜单行。
 */
function openEdit(row) {
  Object.assign(form, {
    id: row.id,
    menuName: row.menuName,
    path: row.path || null,
    apiPath: row.path ? row.apiPath || null : null,
    icon: row.icon || '',
    sortOrder: Number(row.sortOrder || 0),
    status: row.status
  })
  visible.value = true
}

/**
 * 保存菜单展示字段；分组菜单强制清空 apiPath，防止分组被当作页面发起请求。
 */
async function save() {
  await formRef.value?.validate()
  saving.value = true
  try {
    const payload = {
      menuName: form.menuName,
      icon: form.icon || null,
      sortOrder: Number(form.sortOrder || 0),
      status: form.status,
      apiPath: form.path ? form.apiPath || null : null
    }
    await updateMenu(form.id, payload)
    ElMessage.success('菜单已保存')
    visible.value = false
    await load()
  } catch (error) {
    ElMessage.error(error.message || '菜单保存失败')
  } finally {
    saving.value = false
  }
}

onMounted(async () => {
  await Promise.all([load(), loadApiPathOptions()])
})
</script>
