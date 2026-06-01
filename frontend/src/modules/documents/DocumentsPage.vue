<!--
 * @Author: AI Auto Code
 * @Date: 2026-06-01
 * @Description: 文档分析页面
-->
<template>
  <section class="page-section">
    <el-card class="upload-band" shadow="never">
      <el-upload accept=".md,.pdf,.doc,.docx" :auto-upload="false" :on-change="selectFile" :show-file-list="false">
        <el-button :icon="Upload">选择文件</el-button>
      </el-upload>
      <div>
        <strong>{{ selectedFile?.name || '选择 md、pdf、doc、docx 文件' }}</strong>
        <p>上传后可手动触发 AI 分析，解析题目、标准答案和难度星数。</p>
      </div>
      <el-button type="primary" :disabled="!selectedFile" :loading="uploading" @click="submitUpload">上传</el-button>
    </el-card>

    <div class="content-grid">
      <el-card class="document-list" shadow="never">
        <template #header>
          <div class="panel-head">
            <strong>我的文档</strong>
            <el-button :icon="Refresh" @click="loadDocuments">刷新</el-button>
          </div>
        </template>
        <el-button
          v-for="doc in documents"
          :key="doc.id"
          class="document-item"
          :class="{ active: activeDocument?.id === doc.id }"
          text
          @click="selectDocument(doc)"
        >
          <span>
            <strong>{{ doc.originalFilename }}</strong>
            <small>{{ doc.fileType }} · {{ formatSize(doc.fileSize) }} · {{ stateLabel(doc.status) }}</small>
          </span>
        </el-button>
        <el-empty v-if="documents.length === 0" description="暂无我的文档" />
      </el-card>

      <el-card class="analysis-panel" shadow="never">
        <el-empty v-if="!activeDocument" class="empty-state" description="选择或上传一个文档后开始分析" />
        <template v-else>
          <div class="document-head">
            <div>
              <h3>{{ activeDocument.originalFilename }}</h3>
              <p>{{ stateLabel(activeDocument.status) }} · SHA256 {{ activeDocument.sha256 }}</p>
            </div>
            <el-button
              type="primary"
              :disabled="analyzing || !canAnalyzeDocument(activeDocument.status)"
              :loading="analyzing"
              @click="startAnalyze"
            >
              {{ analyzeButtonText(activeDocument.status) }}
            </el-button>
          </div>

          <el-collapse class="preview" @change="loadContent">
            <el-collapse-item title="提取文本预览" name="content">
              <pre>{{ contentPreview || '展开后加载提取文本。' }}</pre>
            </el-collapse-item>
          </el-collapse>

          <div v-if="analysis" class="questions">
            <div class="analysis-meta">
              <strong>分析状态：{{ stateLabel(analysis.status) }}</strong>
              <span>{{ analysis.modelName }}</span>
            </div>
            <div v-if="analysis.chunkProgress" class="tag-line">
              <el-tag>分块 {{ analysis.chunkProgress.total }}</el-tag>
              <el-tag type="success">成功 {{ analysis.chunkProgress.success }}</el-tag>
              <el-tag type="danger">失败 {{ analysis.chunkProgress.failed }}</el-tag>
              <el-tag type="info">待处理 {{ analysis.chunkProgress.pending }}</el-tag>
              <el-tag v-if="analysis.chunkProgress.oversized" type="warning">
                超长 {{ analysis.chunkProgress.oversized }}
              </el-tag>
            </div>
            <el-alert
              v-if="analysis.chunkProgress?.oversized && analysis.chunkProgress?.failed"
              class="inline-alert"
              title="存在超长失败块，建议人工拆分原文后重新上传。"
              type="warning"
              :closable="false"
            />
            <el-card v-for="question in analysis.questions" :key="question.questionId" class="question" shadow="never">
              <div class="question-title">
                <span>#{{ question.questionId }} · {{ question.categoryName }} · {{ typeLabel(question.type) }}</span>
                <el-rate :model-value="question.difficultyStars" disabled />
              </div>
              <div class="tag-line">
                <el-tag>{{ question.newlyCreated ? '新入库' : '复用已有题' }}</el-tag>
                <el-tag type="info">{{ stateLabel(question.state) }}</el-tag>
              </div>
              <h4>{{ question.stem }}</h4>
              <ol v-if="question.options?.length" class="options">
                <li v-for="option in question.options" :key="option">{{ option }}</li>
              </ol>
              <p><strong>标准答案：</strong>{{ question.standardAnswer }}</p>
              <p v-if="question.explanation"><strong>解析：</strong>{{ question.explanation }}</p>
            </el-card>
            <el-empty v-if="analysis.questions.length === 0" description="暂无题目结果" />
          </div>
        </template>
      </el-card>
    </div>
  </section>
</template>

<script setup>
import { onMounted, ref } from 'vue'
import {
  ElAlert,
  ElButton,
  ElCard,
  ElCollapse,
  ElCollapseItem,
  ElEmpty,
  ElMessage,
  ElRate,
  ElTag,
  ElUpload
} from 'element-plus'
import { Refresh, Upload } from '@element-plus/icons-vue'
import { analyzeDocument, getDocumentContent, latestAnalysis, listDocuments as fetchDocuments, uploadDocument } from './api'
import { analyzeButtonText, canAnalyzeDocument, formatSize, stateLabel, typeLabel } from '../../shared/formatters'

const selectedFile = ref(null)
const uploading = ref(false)
const analyzing = ref(false)
const documents = ref([])
const activeDocument = ref(null)
const analysis = ref(null)
const contentPreview = ref('')

function selectFile(uploadFile) {
  selectedFile.value = uploadFile.raw || null
}

async function loadDocuments() {
  try {
    const result = await fetchDocuments()
    documents.value = result?.records || []
  } catch (error) {
    ElMessage.error(error.message || '文档列表加载失败')
  }
}

async function submitUpload() {
  if (!selectedFile.value) return
  uploading.value = true
  try {
    activeDocument.value = await uploadDocument(selectedFile.value)
    analysis.value = null
    contentPreview.value = ''
    selectedFile.value = null
    ElMessage.success('上传成功')
    await loadDocuments()
  } catch (error) {
    ElMessage.error(error.message || '上传失败')
  } finally {
    uploading.value = false
  }
}

async function selectDocument(doc) {
  activeDocument.value = doc
  analysis.value = null
  contentPreview.value = ''
  try {
    analysis.value = await latestAnalysis(doc.id)
  } catch {
    analysis.value = null
  }
}

async function loadContent(activeNames) {
  const opened = Array.isArray(activeNames) ? activeNames.includes('content') : activeNames === 'content'
  if (!opened || !activeDocument.value || contentPreview.value) return
  try {
    const result = await getDocumentContent(activeDocument.value.id)
    contentPreview.value = result?.extractedText || ''
  } catch (error) {
    contentPreview.value = error.message || '文本加载失败'
  }
}

async function startAnalyze() {
  if (!activeDocument.value) return
  analyzing.value = true
  try {
    analysis.value = await analyzeDocument(activeDocument.value.id)
    ElMessage.success('分析完成')
    await loadDocuments()
  } catch (error) {
    ElMessage.error(error.message || '分析失败')
  } finally {
    analyzing.value = false
  }
}

onMounted(loadDocuments)
</script>

