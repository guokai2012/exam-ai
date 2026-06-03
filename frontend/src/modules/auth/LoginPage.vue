<!--
 * @Author: AI Auto Code
 * @Date: 2026-06-01
 * @Description: 登录认证页面
-->
<template>
  <main class="login-page">
    <el-card class="login-card" shadow="never">
      <div class="brand">
        <p class="eyebrow">Exam AI</p>
        <h1>智能出题系统</h1>
      </div>
      <el-form
        ref="loginFormRef"
        class="login-form"
        label-position="top"
        :model="loginForm"
        :rules="loginRules"
        @submit.prevent
      >
        <el-form-item label="账号" prop="username">
          <el-input v-model="loginForm.username" autocomplete="username" clearable placeholder="请输入账号" />
        </el-form-item>
        <el-form-item label="密码" prop="password">
          <el-input
            v-model="loginForm.password"
            autocomplete="current-password"
            placeholder="请输入密码"
            show-password
            type="password"
          />
        </el-form-item>
        <el-form-item label="验证码" prop="captchaCode">
          <div class="captcha-row">
            <el-input v-model="loginForm.captchaCode" placeholder="计算结果" />
            <el-button class="captcha" @click="loadCaptcha">
              <img v-if="captchaImage" :src="captchaImage" alt="验证码" />
              <span v-else>获取</span>
            </el-button>
          </div>
        </el-form-item>
        <el-button class="full-button" :loading="submitting" type="primary" @click="submitLogin">登录</el-button>
        <el-button class="full-button" text @click="router.push('/register')">注册账号</el-button>
      </el-form>
      <p class="status">{{ statusText }}</p>
    </el-card>
  </main>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElButton, ElCard, ElForm, ElFormItem, ElInput, ElMessage } from 'element-plus'
import { currentUser, getCaptcha, login } from './api'
import { setAuthTokens } from '../../shared/authToken'

const router = useRouter()
const route = useRoute()
const submitting = ref(false)
const captchaImage = ref('')
const statusText = ref('请输入账号、密码和图片验证码。')
const loginFormRef = ref(null)
const loginForm = reactive({
  username: '',
  password: '',
  captchaId: '',
  captchaCode: ''
})
const loginRules = {
  username: [{ required: true, message: '请输入账号', trigger: 'blur' }],
  password: [{ required: true, message: '请输入密码', trigger: 'blur' }],
  captchaCode: [{ required: true, message: '请输入验证码', trigger: 'blur' }]
}

async function loadCaptcha() {
  try {
    const result = await getCaptcha()
    loginForm.captchaId = result?.captchaId || ''
    captchaImage.value = result?.imageBase64 || ''
  } catch (error) {
    statusText.value = error.message || '验证码获取失败，请确认后端服务和 Redis 已启动。'
  }
}

async function submitLogin() {
  await loginFormRef.value?.validate()
  submitting.value = true
  statusText.value = '正在登录...'
  try {
    const result = await login(loginForm)
    setAuthTokens(result)
    ElMessage.success('登录成功')
    const me = await currentUser()
    await router.replace(me?.forcePasswordChange ? '/change-password' : route.query.redirect || '/home')
  } catch (error) {
    statusText.value = error.message || '登录失败'
    await loadCaptcha()
  } finally {
    submitting.value = false
  }
}

onMounted(loadCaptcha)
</script>

