<script setup lang="ts">
import { reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, type FormInstance, type FormRules } from 'element-plus'
import { registerTenant } from '@/services/auth'
import type { TenantRegistrationRequest } from '@/types/api'

const router = useRouter()
const formRef = ref<FormInstance>()
const submitting = ref(false)
const form = reactive<TenantRegistrationRequest>({ tenantCode: '', tenantName: '', adminUsername: '', adminPassword: '', adminDisplayName: '', adminEmail: '', adminPhone: '' })
const rules: FormRules<TenantRegistrationRequest> = {
  tenantCode: [{ required: true, message: '请输入企业编码', trigger: 'blur' }, { pattern: /^[A-Za-z][A-Za-z0-9_-]{1,31}$/, message: '以字母开头，只能包含字母、数字、下划线和连字符', trigger: 'blur' }],
  tenantName: [{ required: true, message: '请输入企业名称', trigger: 'blur' }],
  adminUsername: [{ required: true, message: '请输入管理员用户名', trigger: 'blur' }],
  adminPassword: [{ required: true, message: '请输入管理员密码', trigger: 'blur' }, { min: 8, max: 72, message: '密码长度为 8～72 位', trigger: 'blur' }],
  adminDisplayName: [{ required: true, message: '请输入管理员姓名', trigger: 'blur' }],
  adminEmail: [{ type: 'email', message: '邮箱格式不正确', trigger: 'blur' }],
}
async function submit() {
  if (!formRef.value) return
  const valid = await formRef.value.validate().catch(() => false)
  if (!valid) return
  submitting.value = true
  try {
    const result = await registerTenant(form)
    ElMessage.success('企业创建成功，请使用管理员账号登录')
    await router.replace({ name: 'login', query: { tenantCode: result.tenantCode, username: result.adminUsername } })
  } finally { submitting.value = false }
}
</script>
<template><main class="register-page app-background"><section class="register-card glass-card"><div class="login-card__heading"><span class="section-kicker">CREATE WORKSPACE</span><h2>创建企业工作空间</h2><p>系统会自动创建默认部门和首位企业管理员。</p></div><el-form ref="formRef" :model="form" :rules="rules" label-position="top" @submit.prevent="submit"><div class="form-grid"><el-form-item label="企业编码" prop="tenantCode"><el-input v-model="form.tenantCode" placeholder="例如 ACME" /></el-form-item><el-form-item label="企业名称" prop="tenantName"><el-input v-model="form.tenantName" /></el-form-item></div><div class="form-grid"><el-form-item label="管理员用户名" prop="adminUsername"><el-input v-model="form.adminUsername" /></el-form-item><el-form-item label="管理员姓名" prop="adminDisplayName"><el-input v-model="form.adminDisplayName" /></el-form-item></div><el-form-item label="管理员密码" prop="adminPassword"><el-input v-model="form.adminPassword" type="password" show-password /></el-form-item><div class="form-grid"><el-form-item label="联系邮箱" prop="adminEmail"><el-input v-model="form.adminEmail" /></el-form-item><el-form-item label="联系电话" prop="adminPhone"><el-input v-model="form.adminPhone" /></el-form-item></div><el-button class="login-submit" type="primary" native-type="submit" :loading="submitting">创建企业</el-button></el-form><RouterLink class="auth-switch-link" to="/login">已有账号？返回登录</RouterLink><p class="registration-notice">公开注册适合本地和演示环境；生产环境应配合验证码、邮箱验证和审核。</p></section></main></template>
