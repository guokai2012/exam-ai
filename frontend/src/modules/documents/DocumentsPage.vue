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
        <el-pagination
          class="table-pagination"
          size="small"
          background
          layout="prev, pager, next"
          :current-page="pagination.page"
          :page-size="pagination.size"
          :total="pagination.total"
          @current-change="handlePageChange"
        />
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
          <div v-if="showRenderProgress(activeDocument)" class="render-progress-panel">
            <div class="render-progress-meta">
              <strong>{{ renderProgressTitle(activeDocument) }}</strong>
              <span>{{ renderPageLabel(activeDocument) }}</span>
            </div>
            <el-progress
              :percentage="renderProgressPercentage(activeDocument)"
              :status="renderProgressStatus(activeDocument)"
            />
          </div>
          <el-descriptions class="detail-descriptions" :column="2" border>
            <el-descriptions-item label="文档 ID">{{ activeDocument.id }}</el-descriptions-item>
            <el-descriptions-item label="上传人">{{ activeDocument.createId }}</el-descriptions-item>
            <el-descriptions-item label="文件类型">{{ activeDocument.fileType }}</el-descriptions-item>
            <el-descriptions-item label="文件大小">{{ formatSize(activeDocument.fileSize) }}</el-descriptions-item>
            <el-descriptions-item label="总页数">{{ activeDocument.pageCount || '-' }}</el-descriptions-item>
            <el-descriptions-item label="已分片页数">{{ renderedPageCount(activeDocument) }}</el-descriptions-item>
            <el-descriptions-item label="创建时间">{{ activeDocument.createdAt || '-' }}</el-descriptions-item>
            <el-descriptions-item label="最新分析">
              {{ activeDocument.latestAnalysis?.status ? stateLabel(activeDocument.latestAnalysis.status) : '-' }}
            </el-descriptions-item>
          </el-descriptions>

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
              <el-tag v-if="analysis.chunkProgress.processing" type="warning">
                处理中 {{ analysis.chunkProgress.processing }}
              </el-tag>
            </div>
            <el-alert
              v-if="analysis.chunkProgress?.latestErrorMessage"
              class="inline-alert"
              :title="analysis.chunkProgress.latestErrorMessage"
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
import { onMounted, onUnmounted, ref } from 'vue'
import {
  ElAlert,
  ElButton,
  ElCard,
  ElCollapse,
  ElCollapseItem,
  ElDescriptions,
  ElDescriptionsItem,
  ElEmpty,
  ElMessage,
  ElPagination,
  ElProgress,
  ElRate,
  ElTag,
  ElUpload
} from 'element-plus'
import { Refresh, Upload } from '@element-plus/icons-vue'
import { analyzeDocument, getDocumentContent, getDocumentDetail, latestAnalysis, listDocuments as fetchDocuments, uploadDocument } from './api'
import { analyzeButtonText, canAnalyzeDocument, formatSize, stateLabel, typeLabel } from '../../shared/formatters'
import { PAGE_DEFAULTS } from '../../shared/constants'

const selectedFile = ref(null)
const uploading = ref(false)
const analyzing = ref(false)
const documents = ref([])
const activeDocument = ref(null)
const analysis = ref(null)
const contentPreview = ref('')
const pagination = ref({ page: PAGE_DEFAULTS.page, size: PAGE_DEFAULTS.documentSize, total: 0 })
const RENDER_POLL_INTERVAL = 3000
let renderPollingTimer = null

function selectFile(uploadFile) {
  selectedFile.value = uploadFile.raw || null
}

/**
 * 分页加载当前用户文档列表，列表总数用于驱动左侧分页控件。
 */
async function loadDocuments() {
  try {
    const result = await fetchDocuments(pagination.value.page, pagination.value.size)
    documents.value = result?.records || []
    pagination.value.total = Number(result?.total || 0)
    syncActiveDocumentFromList()
    scheduleRenderPolling()
  } catch (error) {
    ElMessage.error(error.message || '文档列表加载失败')
  }
}

/**
 * 上传文档后清空本地选择并刷新列表，让新文档立即出现在当前工作区。
 */
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
    scheduleRenderPolling()
  } catch (error) {
    ElMessage.error(error.message || '上传失败')
  } finally {
    uploading.value = false
  }
}

/**
 * 选中文档时优先加载详情接口，再加载最新分析结果，保证详情面板字段完整。
 */
async function selectDocument(doc) {
  analysis.value = null
  contentPreview.value = ''
  try {
    activeDocument.value = await getDocumentDetail(doc.id)
    try {
      analysis.value = await latestAnalysis(doc.id)
    } catch {
      analysis.value = null
    }
    scheduleRenderPolling()
  } catch {
    analysis.value = null
    activeDocument.value = doc
    scheduleRenderPolling()
  }
}

/**
 * 切换文档页码后刷新列表，当前详情保持不变，避免用户正在查看的分析被打断。
 */
function handlePageChange(page) {
  pagination.value.page = page
  loadDocuments()
}

/**
 * 懒加载提取文本，只有用户展开预览区域时才请求大文本内容。
 */
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

/**
 * 触发 AI 分析并刷新列表状态，便于用户看到文档解析状态变化。
 */
async function startAnalyze() {
  if (!activeDocument.value) return
  analyzing.value = true
  try {
    analysis.value = await analyzeDocument(activeDocument.value.id)
    ElMessage.success('分析完成')
    activeDocument.value = await getDocumentDetail(activeDocument.value.id)
    await loadDocuments()
  } catch (error) {
    ElMessage.error(error.message || '分析失败')
  } finally {
    analyzing.value = false
    scheduleRenderPolling()
  }
}

/**
 * 当前文档处于页图片生成阶段时，启用 3 秒轮询以刷新真实页级进度。
 */
function scheduleRenderPolling() {
  stopRenderPolling()
  if (!isRenderPollingStatus(activeDocument.value?.status)) return
  renderPollingTimer = window.setInterval(refreshActiveDocumentProgress, RENDER_POLL_INTERVAL)
}

/**
 * 清理页图片分片进度轮询，避免组件卸载或状态结束后继续请求。
 */
function stopRenderPolling() {
  if (!renderPollingTimer) return
  window.clearInterval(renderPollingTimer)
  renderPollingTimer = null
}

/**
 * 刷新当前文档详情和列表状态，确保进度条页数与后端最新分片保持一致。
 */
async function refreshActiveDocumentProgress() {
  if (!activeDocument.value) {
    stopRenderPolling()
    return
  }
  try {
    activeDocument.value = await getDocumentDetail(activeDocument.value.id)
    await loadDocuments()
    if (!isRenderPollingStatus(activeDocument.value.status)) {
      stopRenderPolling()
    }
  } catch (error) {
    stopRenderPolling()
    ElMessage.error(error.message || '文档分片进度刷新失败')
  }
}

/**
 * 文档列表刷新后同步当前详情的轻量状态字段，让左侧和右侧状态保持一致。
 */
function syncActiveDocumentFromList() {
  if (!activeDocument.value) return
  const latestDocument = documents.value.find((doc) => doc.id === activeDocument.value.id)
  if (!latestDocument) return
  activeDocument.value = { ...activeDocument.value, ...latestDocument }
}

function isRenderPollingStatus(status) {
  return ['UPLOADED', 'PAGE_RENDERING'].includes(status)
}

function showRenderProgress(doc) {
  return ['UPLOADED', 'PAGE_RENDERING', 'PAGE_READY', 'PAGE_RENDER_FAILED'].includes(doc?.status)
}

function renderedPageCount(doc) {
  if (!doc) return 0
  const total = Number(doc.pageCount || 0)
  if (doc.status === 'PAGE_READY') return total
  return Number(doc.renderedPageCount || 0)
}

function renderPageLabel(doc) {
  const total = Number(doc?.pageCount || 0)
  return `${renderedPageCount(doc)} / ${total || '-'} 页`
}

function renderProgressPercentage(doc) {
  if (doc?.status === 'PAGE_READY') return 100
  const percent = Number(doc?.renderProgressPercent || 0)
  return Math.min(100, Math.max(0, percent))
}

function renderProgressStatus(doc) {
  if (doc?.status === 'PAGE_READY') return 'success'
  if (doc?.status === 'PAGE_RENDER_FAILED') return 'exception'
  return undefined
}

function renderProgressTitle(doc) {
  return {
    UPLOADED: '等待后端开始分片',
    PAGE_RENDERING: '文档分片中，请稍候',
    PAGE_READY: '分片完成，可以发起 AI 解析',
    PAGE_RENDER_FAILED: '文档分片失败，请重新上传或联系管理员'
  }[doc?.status] || '文档分片进度'
}

onMounted(loadDocuments)
onUnmounted(stopRenderPolling)
</script>

