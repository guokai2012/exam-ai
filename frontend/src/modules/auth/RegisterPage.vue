<!--
 * @Author: AI Auto Code
 * @Date: 2026-06-01
 * @Description: 学生账号注册页面
-->
<template>
  <main class="login-page">
    <el-card class="login-card" shadow="never">
      <div class="brand">
        <p class="eyebrow">Register</p>
        <h1>注册学生账号</h1>
      </div>
      <el-form ref="formRef" class="login-form" label-position="top" :model="form" :rules="rules" @submit.prevent>
        <el-form-item label="账号" prop="username">
          <el-input v-model="form.username" autocomplete="username" clearable placeholder="3 到 64 位账号" />
        </el-form-item>
        <el-form-item label="昵称" prop="nickname">
          <el-input v-model="form.nickname" clearable placeholder="请输入昵称" />
        </el-form-item>
        <el-form-item label="密码" prop="password">
          <el-input v-model="form.password" autocomplete="new-password" show-password type="password" placeholder="至少 6 位" />
        </el-form-item>
        <el-form-item label="确认密码" prop="confirmPassword">
          <el-input v-model="form.confirmPassword" autocomplete="new-password" show-password type="password" />
        </el-form-item>
        <el-button class="full-button" type="primary" :loading="submitting" @click="submitRegister">注册</el-button>
        <el-button class="full-button" text @click="router.replace('/login')">返回登录</el-button>
      </el-form>
      <p class="status">{{ statusText }}</p>
    </el-card>
  </main>
</template>

<script setup>
import { reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElButton, ElCard, ElForm, ElFormItem, ElInput, ElMessage } from 'element-plus'
import { register } from './api'
import { VALIDATION_LIMITS } from '../../shared/constants'

const router = useRouter()
const formRef = ref(null)
const submitting = ref(false)
const statusText = ref('注册成功后可直接返回登录。')
const form = reactive({ username: '', nickname: '', password: '', confirmPassword: '' })
const rules = {
  username: [
    { required: true, message: '请输入账号', trigger: 'blur' },
    {
      min: VALIDATION_LIMITS.usernameMin,
      max: VALIDATION_LIMITS.accountMax,
      message: `账号长度为 ${VALIDATION_LIMITS.usernameMin} 到 ${VALIDATION_LIMITS.accountMax} 位`,
      trigger: 'blur'
    }
  ],
  nickname: [{ required: true, message: '请输入昵称', trigger: 'blur' }],
  password: [
    { required: true, message: '请输入密码', trigger: 'blur' },
    {
      min: VALIDATION_LIMITS.passwordMin,
      max: VALIDATION_LIMITS.accountMax,
      message: `密码长度为 ${VALIDATION_LIMITS.passwordMin} 到 ${VALIDATION_LIMITS.accountMax} 位`,
      trigger: 'blur'
    }
  ],
  confirmPassword: [
    {
      validator: (rule, value, callback) => {
        if (!value) {
          callback(new Error('请再次输入密码'))
          return
        }
        if (value !== form.password) {
          callback(new Error('两次输入的密码不一致'))
          return
        }
        callback()
      },
      trigger: 'blur'
    }
  ]
}

/**
 * 提交学生自助注册表单，后端会创建学生账号并绑定默认学生角色。
 */
async function submitRegister() {
  await formRef.value?.validate()
  submitting.value = true
  try {
    await register({ username: form.username, nickname: form.nickname, password: form.password })
    ElMessage.success('注册成功，请登录')
    await router.replace('/login')
  } catch (error) {
    statusText.value = error.message || '注册失败'
  } finally {
    submitting.value = false
  }
}
</script>
