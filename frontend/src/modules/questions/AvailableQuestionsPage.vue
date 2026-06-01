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
            <el-tag type="success">{{ stateLabel(question.state) }}</el-tag>
            <el-tag v-for="tag in question.tags" :key="tag" type="success">{{ tag }}</el-tag>
          </div>
        </el-card>
        <el-empty v-if="questions.length === 0" description="暂无可用题" />
      </div>
    </el-card>
  </section>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import { ElCard, ElEmpty, ElForm, ElFormItem, ElMessage, ElOption, ElRate, ElSelect, ElTag } from 'element-plus'
import { listCategories, listQuestions as fetchQuestions } from './api'
import { stateLabel, typeLabel } from '../../shared/formatters'

const categories = ref([])
const questions = ref([])
const filters = reactive({ categoryId: '' })

async function loadCategories() {
  try {
    categories.value = await listCategories()
  } catch (error) {
    ElMessage.error(error.message || '分类加载失败')
  }
}

async function loadQuestions() {
  try {
    const result = await fetchQuestions({ categoryId: filters.categoryId, state: 'AVAILABLE' })
    questions.value = result?.records || []
  } catch (error) {
    ElMessage.error(error.message || '可用题加载失败')
  }
}

onMounted(async () => {
  await loadCategories()
  await loadQuestions()
})
</script>

