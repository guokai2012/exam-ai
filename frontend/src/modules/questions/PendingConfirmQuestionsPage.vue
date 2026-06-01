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
            <el-select v-model="filters.categoryId" clearable placeholder="全部分类" @change="loadQuestions">
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
            <el-button type="primary" @click="submitReview(question, true)">通过</el-button>
            <el-button @click="submitReview(question, false)">驳回</el-button>
          </div>
          <el-alert v-if="question.reviewReason" class="inline-alert" :title="question.reviewReason" type="info" :closable="false" />
        </el-card>
        <el-empty v-if="questions.length === 0" description="暂无待确认题" />
      </div>
    </el-card>
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
  ElRate,
  ElSelect,
  ElTag
} from 'element-plus'
import { listCategories, listQuestions as fetchQuestions, reviewQuestion } from './api'
import { stateLabel, typeLabel } from '../../shared/formatters'

const categories = ref([])
const questions = ref([])
const filters = reactive({ categoryId: '' })
const reviewDrafts = reactive({})

async function loadCategories() {
  try {
    categories.value = await listCategories()
  } catch (error) {
    ElMessage.error(error.message || '分类加载失败')
  }
}

async function loadQuestions() {
  try {
    const result = await fetchQuestions({ categoryId: filters.categoryId, state: 'PARSE_PENDING_CONFIRM' })
    questions.value = result?.records || []
    for (const question of questions.value) {
      reviewDrafts[question.id] = question.categoryId
    }
  } catch (error) {
    ElMessage.error(error.message || '待确认题加载失败')
  }
}

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

