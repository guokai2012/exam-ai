<!--
 * @Author: AI Auto Code
 * @Date: 2026-06-01
 * @Description: 修改密码页面
-->
<template>
  <main class="login-page">
    <el-card class="login-card" shadow="never">
      <div class="brand">
        <p class="eyebrow">Password Required</p>
        <h1>首次登录请修改密码</h1>
      </div>
      <el-alert class="inline-alert" title="管理员创建或重置密码后，需要先修改密码才能继续使用系统。" type="warning" :closable="false" />
      <el-form
        ref="formRef"
        label-position="top"
        :model="form"
        :rules="formRules"
        @submit.prevent
      >
        <el-form-item label="原密码" prop="oldPassword">
          <el-input v-model="form.oldPassword" show-password type="password" />
        </el-form-item>
        <el-form-item label="新密码" prop="newPassword">
          <el-input v-model="form.newPassword" show-password type="password" />
        </el-form-item>
        <el-form-item label="确认新密码" prop="confirmPassword">
          <el-input v-model="form.confirmPassword" show-password type="password" />
        </el-form-item>
        <el-button class="full-button" type="primary" :loading="submitting" @click="submit">修改密码</el-button>
      </el-form>
    </el-card>
  </main>
</template>

<script setup>
import { reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElAlert, ElButton, ElCard, ElForm, ElFormItem, ElInput, ElMessage } from 'element-plus'
import { changePassword } from '../auth/api'

const router = useRouter()
const submitting = ref(false)
const formRef = ref(null)
const form = reactive({ oldPassword: '', newPassword: '', confirmPassword: '' })
const formRules = {
  oldPassword: [{ required: true, message: '请输入原密码', trigger: 'blur' }],
  newPassword: [
    { required: true, message: '请输入新密码', trigger: 'blur' },
    { min: 8, message: '新密码至少 8 位', trigger: 'blur' }
  ],
  confirmPassword: [
    {
      validator: (rule, value, callback) => {
        if (!form.confirmPassword) {
          callback(new Error('请再次输入新密码'))
          return
        }
        if (form.newPassword !== form.confirmPassword) {
          callback(new Error('两次输入的新密码不一致'))
          return
        }
        callback()
      },
      trigger: 'blur'
    }
  ]
}

async function submit() {
  await formRef.value?.validate()
  if (form.newPassword !== form.confirmPassword) {
    ElMessage.error('两次输入的新密码不一致')
    return
  }
  submitting.value = true
  try {
    await changePassword({
      oldPassword: form.oldPassword,
      newPassword: form.newPassword
    })
    ElMessage.success('密码已修改')
    await router.replace('/documents')
  } catch (error) {
    ElMessage.error(error.message || '密码修改失败')
  } finally {
    submitting.value = false
  }
}
</script>

