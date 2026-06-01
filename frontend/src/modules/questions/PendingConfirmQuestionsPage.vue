<!--
 * @Author: AI Auto Code
 * @Date: 2026-06-01
 * @Description: 待确认题管理页面
-->
<template>
  <section class="page-section">
    <el-card class="question-bank" shadow="never">
      <div class="toolbar compact-toolbar">
        <div>
          <p class="eyebrow">Pending Confirmation</p>
          <h2>待确认题</h2>
        </div>
        <el-form class="filter-form" inline>
          <el-form-item label="分类">
            <el-select v-model="filters.categoryId" clearable placeholder="全部分类" @change="handleFilterChange">
              <el-option v-for="category in categories" :key="category.id" :label="category.categoryName" :value="category.id" />
            </el-select>
          </el-form-item>
        </el-form>
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
            <el-tag type="warning">{{ stateLabel(question.state) }}</el-tag>
          </div>
          <div class="review-row">
            <el-select v-model="reviewDrafts[question.id]" placeholder="选择分类">
              <el-option v-for="category in categories" :key="category.id" :label="category.categoryName" :value="category.id" />
            </el-select>
            <el-button plain @click="openQuestionDetail(question)">详情</el-button>
            <el-button type="primary" @click="submitReview(question, true)">通过</el-button>
            <el-button @click="submitReview(question, false)">驳回</el-button>
          </div>
          <el-alert v-if="question.reviewReason" class="inline-alert" :title="question.reviewReason" type="info" :closable="false" />
        </el-card>
        <el-empty v-if="questions.length === 0" description="暂无待确认题" />
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
    <QuestionDetailDialog v-model="detailVisible" :question="detailQuestion" />
  </section>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import {
  ElAlert,
  ElButton,
  ElCard,
  ElEmpty,
  ElForm,
  ElFormItem,
  ElMessage,
  ElOption,
  ElPagination,
  ElRate,
  ElSelect,
  ElTag
} from 'element-plus'
import { getQuestion, listCategories, listQuestions as fetchQuestions, reviewQuestion } from './api'
import { stateLabel, typeLabel } from '../../shared/formatters'
import { PAGE_DEFAULTS } from '../../shared/constants'
import QuestionDetailDialog from './QuestionDetailDialog.vue'

const categories = ref([])
const questions = ref([])
const filters = reactive({ categoryId: '' })
const pagination = reactive({ page: PAGE_DEFAULTS.page, size: PAGE_DEFAULTS.size, total: 0 })
const reviewDrafts = reactive({})
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
      state: 'PARSE_PENDING_CONFIRM',
      page: pagination.page,
      size: pagination.size
    })
    questions.value = result?.records || []
    pagination.total = Number(result?.total || 0)
    for (const question of questions.value) {
      reviewDrafts[question.id] = question.categoryId
    }
  } catch (error) {
    ElMessage.error(error.message || '待确认题加载失败')
  }
}

/**
 * 分类筛选变化后重置到第一页，确保待确认列表和分页总数重新计算。
 */
function handleFilterChange() {
  pagination.page = PAGE_DEFAULTS.page
  loadQuestions()
}

/**
 * 翻页时保留当前分类筛选和待确认状态条件。
 */
function handlePageChange(page) {
  pagination.page = page
  loadQuestions()
}

/**
 * 每页数量变化会影响总页数，因此统一回到第一页重新查询。
 */
function handleSizeChange(size) {
  pagination.size = size
  pagination.page = PAGE_DEFAULTS.page
  loadQuestions()
}

/**
 * 详情弹窗使用后端详情接口获取完整题目信息，避免只展示列表摘要。
 */
async function openQuestionDetail(question) {
  try {
    detailQuestion.value = await getQuestion(question.id)
    detailVisible.value = true
  } catch (error) {
    ElMessage.error(error.message || '题目详情加载失败')
  }
}

/**
 * 提交题目确认或驳回结果，并在成功后刷新待确认列表。
 */
async function submitReview(question, approved) {
  try {
    await reviewQuestion(question.id, {
      approved,
      categoryId: reviewDrafts[question.id] || question.categoryId,
      reason: approved ? '审核通过' : '审核驳回'
    })
    ElMessage.success('审核操作已提交')
    await loadQuestions()
  } catch (error) {
    ElMessage.error(error.message || '审核失败')
  }
}

onMounted(async () => {
  await loadCategories()
  await loadQuestions()
})
</script>

