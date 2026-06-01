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
        :page-sizes="[10, 20, 50]"
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
import QuestionDetailDialog from './QuestionDetailDialog.vue'

const categories = ref([])
const questions = ref([])
const filters = reactive({ categoryId: '' })
const pagination = reactive({ page: 1, size: 20, total: 0 })
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

function handleFilterChange() {
  pagination.page = 1
  loadQuestions()
}

function handlePageChange(page) {
  pagination.page = page
  loadQuestions()
}

function handleSizeChange(size) {
  pagination.size = size
  pagination.page = 1
  loadQuestions()
}

function openCategoryDialog() {
  Object.assign(categoryForm, { categoryName: '', description: '' })
  categoryVisible.value = true
  categoryFormRef.value?.clearValidate()
}

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

