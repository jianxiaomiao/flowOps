<script setup lang="ts">
import { computed, onMounted, onUnmounted, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, type FormInstance, type FormRules } from 'element-plus'
import { RefreshRight } from '@element-plus/icons-vue'
import { fetchCaptcha, registerTenant, sendRegisterEmailCode } from '@/services/auth'
import { ApiError } from '@/services/http'
import type { TenantRegistrationRequest } from '@/types/api'

const router = useRouter()
const formRef = ref<FormInstance>()
const submitting = ref(false)
const sendingCode = ref(false)
const loadingCaptcha = ref(false)
const captchaId = ref('')
const captchaImage = ref('')
const captchaAnswer = ref('')
const captchaConsumed = ref(false)
const cooldown = ref(0)
let cooldownTimer: number | undefined

const form = reactive<TenantRegistrationRequest>({
  tenantCode: '',
  tenantName: '',
  adminUsername: '',
  adminPassword: '',
  adminDisplayName: '',
  adminEmail: '',
  adminPhone: '',
  emailCode: '',
})

const rules: FormRules<TenantRegistrationRequest> = {
  tenantCode: [
    { required: true, message: '请输入企业编码', trigger: 'blur' },
    { pattern: /^[A-Za-z][A-Za-z0-9_-]{1,31}$/, message: '以字母开头，只能包含字母、数字、下划线和连字符', trigger: 'blur' },
  ],
  tenantName: [{ required: true, message: '请输入企业名称', trigger: 'blur' }],
  adminUsername: [{ required: true, message: '请输入管理员用户名', trigger: 'blur' }],
  adminPassword: [
    { required: true, message: '请输入管理员密码', trigger: 'blur' },
    { min: 8, max: 72, message: '密码长度为 8～72 位', trigger: 'blur' },
  ],
  adminDisplayName: [{ required: true, message: '请输入管理员姓名', trigger: 'blur' }],
  adminEmail: [
    { required: true, message: '请输入联系邮箱', trigger: 'blur' },
    { type: 'email', message: '邮箱格式不正确', trigger: 'blur' },
  ],
  emailCode: [
    { required: true, message: '请输入 6 位邮箱验证码', trigger: 'blur' },
    { pattern: /^\d{6}$/, message: '请输入 6 位数字验证码', trigger: 'blur' },
  ],
}

const sendButtonLabel = computed(() => {
  if (cooldown.value > 0) return `${cooldown.value}s 后重试`
  return '获取验证码'
})

function clearCooldownTimer() {
  if (cooldownTimer !== undefined) {
    window.clearInterval(cooldownTimer)
    cooldownTimer = undefined
  }
}

function startCooldown(seconds: number) {
  clearCooldownTimer()
  cooldown.value = seconds
  cooldownTimer = window.setInterval(() => {
    if (cooldown.value <= 1) {
      cooldown.value = 0
      clearCooldownTimer()
    } else {
      cooldown.value -= 1
    }
  }, 1000)
}

async function loadCaptcha() {
  loadingCaptcha.value = true
  try {
    const data = await fetchCaptcha()
    captchaId.value = data.captchaId
    captchaImage.value = data.imageDataUrl
    captchaAnswer.value = ''
    captchaConsumed.value = false
  } catch (error) {
    ElMessage.error(error instanceof ApiError ? error.message : '图形验证码加载失败，请重试')
  } finally {
    loadingCaptcha.value = false
  }
}

async function handleSendEmailCode() {
  if (cooldown.value > 0 || sendingCode.value) return
  if (!formRef.value) return
  const emailValid = await formRef.value.validateField('adminEmail').then(() => true).catch(() => false)
  if (!emailValid) return
  if (captchaConsumed.value) {
    await loadCaptcha()
    ElMessage.info('请输入新的图形算术验证码后再重新获取')
    return
  }
  if (!captchaId.value || !captchaAnswer.value.trim()) {
    ElMessage.warning('请先输入图形算术验证码答案')
    return
  }

  sendingCode.value = true
  try {
    const result = await sendRegisterEmailCode({
      email: form.adminEmail.trim(),
      captchaId: captchaId.value,
      captchaAnswer: captchaAnswer.value.trim(),
    })
    ElMessage.success('验证码已发送至邮箱，5 分钟内有效')
    captchaConsumed.value = true
    startCooldown(result.cooldownSeconds || 60)
  } catch (error) {
    ElMessage.error(error instanceof ApiError ? error.message : '验证码发送失败，请稍后重试')
    await loadCaptcha()
  } finally {
    sendingCode.value = false
  }
}

async function submit() {
  if (!formRef.value) return
  const valid = await formRef.value.validate().catch(() => false)
  if (!valid) return
  submitting.value = true
  try {
    const result = await registerTenant({
      ...form,
      adminEmail: form.adminEmail.trim(),
      emailCode: form.emailCode.trim(),
    })
    ElMessage.success('企业创建成功，请使用管理员账号登录')
    await router.replace({
      name: 'login',
      query: { tenantCode: result.tenantCode, username: result.adminUsername },
    })
  } catch (error) {
    ElMessage.error(error instanceof ApiError ? error.message : '企业创建失败，请检查信息后重试')
  } finally {
    submitting.value = false
  }
}

onMounted(() => {
  loadCaptcha()
})

onUnmounted(() => {
  clearCooldownTimer()
})
</script>

<template>
  <main class="register-page app-background">
    <section class="register-card glass-card">
      <div class="login-card__heading">
        <span class="section-kicker">CREATE WORKSPACE</span>
        <h2>创建企业工作空间</h2>
        <p>完成邮箱验证后，系统会自动创建默认部门和首位企业管理员。</p>
      </div>

      <el-form ref="formRef" :model="form" :rules="rules" label-position="top" @submit.prevent="submit">
        <div class="form-grid">
          <el-form-item label="企业编码" prop="tenantCode">
            <el-input v-model="form.tenantCode" placeholder="例如 ACME" />
          </el-form-item>
          <el-form-item label="企业名称" prop="tenantName">
            <el-input v-model="form.tenantName" placeholder="例如 星云科技" />
          </el-form-item>
        </div>

        <div class="form-grid">
          <el-form-item label="管理员用户名" prop="adminUsername">
            <el-input v-model="form.adminUsername" placeholder="例如 admin" />
          </el-form-item>
          <el-form-item label="管理员姓名" prop="adminDisplayName">
            <el-input v-model="form.adminDisplayName" placeholder="例如 张明" />
          </el-form-item>
        </div>

        <el-form-item label="管理员密码" prop="adminPassword">
          <el-input v-model="form.adminPassword" type="password" show-password placeholder="8～72 位密码" />
        </el-form-item>

        <div class="form-grid">
          <el-form-item label="联系邮箱" prop="adminEmail">
            <el-input v-model="form.adminEmail" placeholder="用于接收注册验证码" />
          </el-form-item>
          <el-form-item label="联系电话（可选）" prop="adminPhone">
            <el-input v-model="form.adminPhone" placeholder="选填" />
          </el-form-item>
        </div>

        <el-form-item label="图形算术验证码">
          <div class="register-captcha-row">
            <el-input
              v-model="captchaAnswer"
              placeholder="请输入右侧算术题结果"
              maxlength="12"
            />
            <button
              type="button"
              class="register-captcha-button"
              :disabled="loadingCaptcha"
              aria-label="刷新图形验证码"
              @click="loadCaptcha"
            >
              <img
                v-if="captchaImage"
                class="register-captcha-img"
                :src="captchaImage"
                alt="图形算术验证码，点击刷新"
              />
              <span v-else class="register-captcha-placeholder">
                <el-icon><RefreshRight /></el-icon>
                加载中
              </span>
            </button>
          </div>
        </el-form-item>

        <el-form-item label="邮箱验证码" prop="emailCode">
          <div class="register-code-row">
            <el-input
              v-model="form.emailCode"
              placeholder="请输入 6 位数字验证码"
              maxlength="6"
            />
            <el-button
              type="primary"
              plain
              :loading="sendingCode"
              :disabled="cooldown > 0 || loadingCaptcha"
              @click="handleSendEmailCode"
            >
              {{ sendButtonLabel }}
            </el-button>
          </div>
        </el-form-item>

        <el-button class="login-submit" type="primary" native-type="submit" :loading="submitting">
          创建企业
        </el-button>
      </el-form>

      <RouterLink class="auth-switch-link" to="/login">已有账号？返回登录</RouterLink>
      <p class="registration-notice">图形验证码一次性有效，点击图片可刷新；邮箱验证码 5 分钟内有效，最多允许输错 5 次。</p>
    </section>
  </main>
</template>
