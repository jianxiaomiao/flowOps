import { createApp } from 'vue'
import {
  ElBadge,
  ElButton,
  ElCheckbox,
  ElDescriptions,
  ElDescriptionsItem,
  ElDialog,
  ElDrawer,
  ElForm,
  ElFormItem,
  ElIcon,
  ElInput,
  ElInputNumber,
  ElLoading,
  ElMessage,
  ElOption,
  ElPagination,
  ElRadio,
  ElRadioGroup,
  ElSelect,
  ElSwitch,
  ElTable,
  ElTableColumn,
  ElTabPane,
  ElTabs,
  ElTag,
  ElTimeline,
  ElTimelineItem,
} from 'element-plus'
import 'element-plus/dist/index.css'
import './assets/style.css'
import App from './App.vue'
import router from './router'
import { pinia } from './stores'
import { useAuthStore } from './stores/auth'
import { ApiError } from './services/http'

const app = createApp(App)
const elementComponents = [
  ElBadge,
  ElButton,
  ElCheckbox,
  ElDescriptions,
  ElDescriptionsItem,
  ElDialog,
  ElDrawer,
  ElForm,
  ElFormItem,
  ElIcon,
  ElInput,
  ElInputNumber,
  ElOption,
  ElPagination,
  ElRadio,
  ElRadioGroup,
  ElSelect,
  ElSwitch,
  ElTable,
  ElTableColumn,
  ElTabPane,
  ElTabs,
  ElTag,
  ElTimeline,
  ElTimelineItem,
]

elementComponents.forEach((component) => app.use(component))
app.use(ElLoading)

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
  .mount('#app')
