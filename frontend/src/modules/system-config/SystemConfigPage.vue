<!--
 * @Author: AI Auto Code
 * @Date: 2026-06-01
 * @Description: 系统配置页面
-->
<template>
  <section class="page-section">
    <el-card class="admin-panel" shadow="never">
      <div class="panel-head">
        <div>
          <p class="eyebrow">System Config</p>
          <h2>系统配置</h2>
        </div>
        <el-button :icon="Refresh" @click="loadConfigs">刷新</el-button>
      </div>
      <el-empty v-if="configs.length === 0" description="暂无可管理配置，或当前账号无权限" />
      <div v-for="config in configs" :key="config.configKey" class="config-row">
        <div>
          <strong>{{ config.configName }}</strong>
          <p>{{ config.description }}</p>
        </div>
        <el-input v-model="drafts[config.configKey]" inputmode="numeric" />
        <el-button type="primary" @click="saveConfig(config)">保存</el-button>
      </div>
    </el-card>
  </section>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import { ElButton, ElCard, ElEmpty, ElInput, ElMessage } from 'element-plus'
import { Refresh } from '@element-plus/icons-vue'
import { listSystemConfigs, updateSystemConfig } from './api'

const configs = ref([])
const drafts = reactive({})

async function loadConfigs() {
  try {
    configs.value = await listSystemConfigs()
    for (const config of configs.value) {
      drafts[config.configKey] = config.configValue
    }
  } catch (error) {
    configs.value = []
    ElMessage.error(error.message || '系统配置加载失败')
  }
}

async function saveConfig(config) {
  try {
    const result = await updateSystemConfig(config.configKey, drafts[config.configKey])
    ElMessage.success(result.message || '系统配置已保存')
    await loadConfigs()
  } catch (error) {
    ElMessage.error(error.message || '系统配置保存失败')
  }
}

onMounted(loadConfigs)
</script>

