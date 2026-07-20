import { createApp } from 'vue'
import ElementPlus, { ElMessage } from 'element-plus'
import 'element-plus/dist/index.css'
import './assets/style.css'
import App from './App.vue'
import router from './router'
import { pinia } from './stores'
import { useAuthStore } from './stores/auth'
import { ApiError } from './services/http'

const app = createApp(App)

app.config.errorHandler = (error) => {
  if (error instanceof ApiError) {
    if (error.code === 10002 || error.httpStatus === 401) {
      useAuthStore(pinia).logout()
      router.replace('/login')
    }
    const traceHint = error.traceId ? `（TraceId: ${error.traceId}）` : ''
    ElMessage.error(`${error.message}${traceHint}`)
    return
  }
  ElMessage.error('页面操作失败，请稍后重试')
}

app
  .use(pinia)
  .use(router)
  .use(ElementPlus)
  .mount('#app')
