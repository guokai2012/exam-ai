<!--
 * @Author: AI Auto Code
 * @Date: 2026-06-01
 * @Description: 可用题管理页面
-->
<template>
  <section class="page-section">
    <el-card class="question-bank" shadow="never">
      <div class="toolbar compact-toolbar">
        <div>
          <p class="eyebrow">Available Questions</p>
          <h2>可用题</h2>
        </div>
        <div class="filter-form">
          <el-button type="primary" @click="openCategoryDialog">新增分类</el-button>
          <el-form inline>
            <el-form-item label="分类">
              <el-select v-model="filters.categoryId" clearable placeholder="全部分类" @change="handleFilterChange">
                <el-option v-for="category in categories" :key="category.id" :label="category.categoryName" :value="category.id" />
              </el-select>
            </el-form-item>
          </el-form>
        </div>
      </div>
      <div class="bank-list">
        <el-card v-for="question in questions" :key="question.id" class="bank-item" shadow="never">
          <div class="question-title">
            <span>#{{ question.id }} · {{ question.categoryName }} · {{ typeLabel(question.questionType) }}</span>
            <el-rate :model-value="question.difficultyStars" disabled />
          </div>
          <h4>{{ question.stem }}</h4>
          <p><strong>标准答案：</strong>{{ question.standardAnswer }}</p>
          <div class="tag-line">
            <el-tag type="success">{{ stateLabel(question.state) }}</el-tag>
            <el-tag v-for="tag in question.tags" :key="tag" type="success">{{ tag }}</el-tag>
          </div>
          <el-button plain @click="openQuestionDetail(question)">查看详情</el-button>
        </el-card>
        <el-empty v-if="questions.length === 0" description="暂无可用题" />
      </div>
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

    <el-dialog v-model="categoryVisible" title="新增题目分类" width="460px">
      <el-form ref="categoryFormRef" label-position="top" :model="categoryForm" :rules="categoryRules">
        <el-form-item label="分类名称" prop="categoryName">
          <el-input v-model="categoryForm.categoryName" />
        </el-form-item>
        <el-form-item label="说明">
          <el-input v-model="categoryForm.description" type="textarea" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="categoryVisible = false">取消</el-button>
        <el-button type="primary" @click="saveCategory">保存</el-button>
      </template>
    </el-dialog>
    <QuestionDetailDialog v-model="detailVisible" :question="detailQuestion" />
  </section>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import {
  ElButton,
  ElCard,
  ElDialog,
  ElEmpty,
  ElForm,
  ElFormItem,
  ElInput,
  ElMessage,
  ElOption,
  ElPagination,
  ElRate,
  ElSelect,
  ElTag
} from 'element-plus'
import { createCategory, getQuestion, listCategories, listQuestions as fetchQuestions } from './api'
import { stateLabel, typeLabel } from '../../shared/formatters'
import { PAGE_DEFAULTS } from '../../shared/constants'
import QuestionDetailDialog from './QuestionDetailDialog.vue'

const categories = ref([])
const questions = ref([])
const filters = reactive({ categoryId: '' })
const pagination = reactive({ page: PAGE_DEFAULTS.page, size: PAGE_DEFAULTS.size, total: 0 })
const categoryVisible = ref(false)
const categoryFormRef = ref(null)
const categoryForm = reactive({ categoryName: '', description: '' })
const categoryRules = {
  categoryName: [{ required: true, message: '请输入分类名称', trigger: 'blur' }]
}
const detailVisible = ref(false)
const detailQuestion = ref(null)

async function loadCategories() {
  try {
    categories.value = await listCategories()
  } catch (error) {
    ElMessage.error(error.message || '分类加载失败')
  }
}

/**
 * 按当前筛选条件分页加载可用题，保证列表、总数和分页控件保持一致。
 */
async function loadQuestions() {
  try {
    const result = await fetchQuestions({
      categoryId: filters.categoryId,
      state: 'AVAILABLE',
      page: pagination.page,
      size: pagination.size
    })
    questions.value = result?.records || []
    pagination.total = Number(result?.total || 0)
  } catch (error) {
    ElMessage.error(error.message || '可用题加载失败')
  }
}

/**
 * 筛选条件变化后回到第一页，避免旧页码在新条件下出现空列表。
 */
function handleFilterChange() {
  pagination.page = PAGE_DEFAULTS.page
  loadQuestions()
}

/**
 * 切换页码后保留当前分类筛选，只刷新题目列表。
 */
function handlePageChange(page) {
  pagination.page = page
  loadQuestions()
}

/**
 * 调整每页数量后回到第一页，避免分页边界变化导致请求越界。
 */
function handleSizeChange(size) {
  pagination.size = size
  pagination.page = PAGE_DEFAULTS.page
  loadQuestions()
}

/**
 * 打开新增分类弹窗前重置草稿，防止上一次输入残留到新建流程。
 */
function openCategoryDialog() {
  Object.assign(categoryForm, { categoryName: '', description: '' })
  categoryVisible.value = true
  categoryFormRef.value?.clearValidate()
}

/**
 * 保存人工维护的题目分类，成功后立即刷新分类下拉供筛选使用。
 */
async function saveCategory() {
  await categoryFormRef.value?.validate()
  try {
    await createCategory({ categoryName: categoryForm.categoryName, description: categoryForm.description })
    ElMessage.success('分类已新增')
    categoryVisible.value = false
    await loadCategories()
  } catch (error) {
    ElMessage.error(error.message || '分类新增失败')
  }
}

/**
 * 按题目 ID 重新获取详情，避免列表摘要字段缺失影响详情弹窗展示。
 */
async function openQuestionDetail(question) {
  try {
    detailQuestion.value = await getQuestion(question.id)
    detailVisible.value = true
  } catch (error) {
    ElMessage.error(error.message || '题目详情加载失败')
  }
}

onMounted(async () => {
  await loadCategories()
  await loadQuestions()
})
</script>

