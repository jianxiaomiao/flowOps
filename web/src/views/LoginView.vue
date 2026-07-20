<script setup lang="ts">
import { reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, type FormInstance, type FormRules } from 'element-plus'
import { ArrowRight, Lock, OfficeBuilding, User } from '@element-plus/icons-vue'
import { useAuthStore } from '@/stores/auth'
import { ApiError } from '@/services/http'
import type { LoginRequest } from '@/types/api'

const router = useRouter()
const route = useRoute()
const authStore = useAuthStore()
const formRef = ref<FormInstance>()
const submitting = ref(false)
const form = reactive<LoginRequest>({
  tenantCode: typeof route.query.tenantCode === 'string' ? route.query.tenantCode : '',
  username: typeof route.query.username === 'string' ? route.query.username : '',
  password: '',
})

const rules: FormRules<LoginRequest> = {
  tenantCode: [{ required: true, message: '请输入企业编码', trigger: 'blur' }],
  username: [{ required: true, message: '请输入用户名', trigger: 'blur' }],
  password: [{ required: true, message: '请输入密码', trigger: 'blur' }],
}

async function submitLogin() {
  if (!formRef.value) return
  await formRef.value.validate(async (valid) => {
    if (!valid) return
    submitting.value = true
    try {
      await authStore.login(form)
      ElMessage.success('欢迎回来')
      const redirect = typeof route.query.redirect === 'string' ? route.query.redirect : '/tickets'
      await router.replace(redirect)
    } catch (error) {
      ElMessage.error(error instanceof ApiError ? error.message : '登录失败，请稍后重试')
    } finally {
      submitting.value = false
    }
  })
}
</script>

<template>
  <main class="login-page app-background">
    <section class="login-hero">
      <div class="brand-lockup brand-lockup--hero">
        <span class="brand-mark">F</span>
        <span>FlowOps</span>
      </div>

      <div class="login-hero__content">
        <span class="eyebrow">WORK FLOWS, MADE LIGHTER</span>
        <h1>让每一次协作，<br />都有清晰的流向。</h1>
        <p>从问题提交到解决闭环，把工单、SLA 与团队协作放进一个轻盈的工作空间。</p>
      </div>

      <div class="palette-strip" aria-label="FlowOps 品牌色">
      </div>
    </section>

    <section class="login-panel-wrap">
      <div class="login-card glass-card">
        <div class="login-card__heading">
          <span class="section-kicker">FLOWOPS CONSOLE</span>
          <h2>登录工作台</h2>
          <p>使用你的企业账号继续</p>
        </div>

        <el-form ref="formRef" :model="form" :rules="rules" label-position="top" @submit.prevent="submitLogin">
          <el-form-item label="企业编码" prop="tenantCode">
            <el-input v-model="form.tenantCode" size="large" placeholder="例如：acme" :prefix-icon="OfficeBuilding" />
          </el-form-item>
          <el-form-item label="用户名" prop="username">
            <el-input v-model="form.username" size="large" placeholder="请输入用户名" :prefix-icon="User" />
          </el-form-item>
          <el-form-item label="密码" prop="password">
            <el-input v-model="form.password" size="large" type="password" show-password placeholder="请输入密码" :prefix-icon="Lock" @keyup.enter="submitLogin" />
          </el-form-item>

          <el-button class="login-submit" type="primary" size="large" :loading="submitting" native-type="submit">
            进入 FlowOps
            <el-icon><ArrowRight /></el-icon>
          </el-button>
        </el-form>

        <p class="login-card__footer">登录即表示你正在访问所在企业的安全工作空间</p>
        <RouterLink class="auth-switch-link" to="/register">还没有企业账号？创建企业</RouterLink>
      </div>
    </section>
  </main>
</template>
