<!--
 * @Author: AI Auto Code
 * @Date: 2026-06-01
 * @Description: 题目详情弹窗组件
-->
<template>
  <el-dialog v-model="visible" title="题目详情" width="720px">
    <el-empty v-if="!question" description="暂无题目详情" />
    <template v-else>
      <el-descriptions :column="1" border>
        <el-descriptions-item label="题目 ID">{{ question.id }}</el-descriptions-item>
        <el-descriptions-item label="分类">{{ question.categoryName || '-' }}</el-descriptions-item>
        <el-descriptions-item label="题型">{{ typeLabel(question.questionType) }}</el-descriptions-item>
        <el-descriptions-item label="状态">{{ stateLabel(question.state) }}</el-descriptions-item>
        <el-descriptions-item label="难度">
          <el-rate :model-value="question.difficultyStars" disabled />
        </el-descriptions-item>
        <el-descriptions-item label="题干">{{ question.stem }}</el-descriptions-item>
        <el-descriptions-item v-if="question.options?.length" label="选项">
          <ol class="options">
            <li v-for="option in question.options" :key="option">{{ option }}</li>
          </ol>
        </el-descriptions-item>
        <el-descriptions-item label="标准答案">{{ question.standardAnswer }}</el-descriptions-item>
        <el-descriptions-item label="解析">{{ question.explanation || '-' }}</el-descriptions-item>
        <el-descriptions-item label="标签">
          <div class="tag-line">
            <el-tag v-for="tag in question.tags || []" :key="tag" type="success">{{ tag }}</el-tag>
            <span v-if="!question.tags?.length">-</span>
          </div>
        </el-descriptions-item>
        <el-descriptions-item label="审核说明">{{ question.reviewReason || '-' }}</el-descriptions-item>
        <el-descriptions-item label="标签状态">
          {{ question.tagErrorMessage || (question.tagNotified ? '已通知标签失败' : '-') }}
        </el-descriptions-item>
        <el-descriptions-item label="创建时间">{{ question.createdAt || '-' }}</el-descriptions-item>
      </el-descriptions>
    </template>
  </el-dialog>
</template>

<script setup>
import { computed } from 'vue'
import { ElDescriptions, ElDescriptionsItem, ElDialog, ElEmpty, ElRate, ElTag } from 'element-plus'
import { stateLabel, typeLabel } from '../../shared/formatters'

const props = defineProps({
  modelValue: {
    type: Boolean,
    default: false
  },
  question: {
    type: Object,
    default: null
  }
})

const emit = defineEmits(['update:modelValue'])
const visible = computed({
  get: () => props.modelValue,
  set: value => emit('update:modelValue', value)
})
</script>
