<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import {
  ArrowRight,
  CircleCheck,
  Plus,
  RefreshRight,
  Search,
  Timer,
  TrendCharts,
  Warning,
} from '@element-plus/icons-vue'
import { ApiError } from '@/services/http'
import { fetchTicketPage } from '@/services/ticket'
import { useAuthStore } from '@/stores/auth'
import type { TicketPreview, TicketStatus } from '@/types/api'

const router = useRouter()
const authStore = useAuthStore()
const keyword = ref('')
const selectedStatus = ref<TicketStatus | ''>('')
const selectedPriority = ref<TicketPreview['priority'] | ''>('')
const tickets = ref<TicketPreview[]>([])
const total = ref(0)
const pageNum = ref(1)
const pageSize = ref(5)
const loading = ref(false)
const pageError = ref('')

const statusLabels: Record<TicketStatus, string> = {
  OPEN: '待分派',
  ASSIGNED: '待接单',
  ACCEPTED: '已接单',
  PROCESSING: '处理中',
  PENDING_CONFIRMATION: '待确认',
  RESOLVED: '已解决',
  CLOSED: '已关闭',
}

const statusOptions = Object.entries(statusLabels).map(([value, label]) => ({
  value: value as TicketStatus,
  label,
}))

const displayName = computed(() => authStore.username || '当前用户')
const todayLabel = new Intl.DateTimeFormat('zh-CN', {
  month: 'long',
  day: 'numeric',
  weekday: 'long',
}).format(new Date())

const activeTickets = computed(() => tickets.value.filter((ticket) => !['RESOLVED', 'CLOSED'].includes(ticket.status)))
const waitingTickets = computed(() => tickets.value.filter((ticket) => ['OPEN', 'ASSIGNED', 'PENDING_CONFIRMATION'].includes(ticket.status)))
const breachedTickets = computed(() => tickets.value.filter((ticket) => ticket.responseSlaStatus === 'BREACHED' || ticket.resolutionSlaStatus === 'BREACHED'))
const pendingSlaTickets = computed(() => tickets.value.filter((ticket) => ticket.responseSlaStatus === 'PENDING' || ticket.resolutionSlaStatus === 'PENDING'))
const healthPercentage = computed(() => {
  if (tickets.value.length === 0) return 100
  return Math.max(0, Math.round(((tickets.value.length - breachedTickets.value.length) / tickets.value.length) * 100))
})
const urgentTicket = computed(() => activeTickets.value.find((ticket) => ticket.priority === 'P0'))

async function loadTickets(resetPage = false) {
  if (resetPage) pageNum.value = 1
  loading.value = true
  pageError.value = ''
  try {
    const page = await fetchTicketPage({
      pageNum: pageNum.value,
      pageSize: pageSize.value,
      status: selectedStatus.value || undefined,
      priority: selectedPriority.value || undefined,
      title: keyword.value.trim() || undefined,
    })
    tickets.value = page.records
    total.value = page.total
    pageNum.value = page.current
    pageSize.value = page.size
  } catch (error) {
    const message = error instanceof ApiError ? error.message : '工单数据加载失败'
    pageError.value = message
    if (error instanceof ApiError && error.code === 10002) {
      authStore.logout()
      await router.replace('/login')
    }
  } finally {
    loading.value = false
  }
}

function handlePageChange(current: number) {
  pageNum.value = current
  loadTickets()
}

function statusClass(status: TicketStatus) {
  return `status-pill status-pill--${status.toLowerCase().replaceAll('_', '-')}`
}

function priorityClass(priority: TicketPreview['priority']) {
  return `priority-badge priority-badge--${priority.toLowerCase()}`
}

function assigneeLabel(assigneeId: number | null) {
  return assigneeId ? `用户 #${assigneeId}` : '未分派'
}

function relativeTime(value: string) {
  const timestamp = new Date(value.replace(' ', 'T')).getTime()
  if (Number.isNaN(timestamp)) return value
  const minutes = Math.max(0, Math.floor((Date.now() - timestamp) / 60_000))
  if (minutes < 1) return '刚刚'
  if (minutes < 60) return `${minutes} 分钟前`
  const hours = Math.floor(minutes / 60)
  if (hours < 24) return `${hours} 小时前`
  return `${Math.floor(hours / 24)} 天前`
}

function showCreateHint() {
  ElMessage.info('真实工单列表已接通，下一步接入创建表单与部门选项')
}

function showDetailHint(ticket: TicketPreview) {
  ElMessage.info(`下一步接入工单详情：${ticket.ticketNo}`)
}

function handleRowClick(row: unknown) {
  showDetailHint(row as TicketPreview)
}

onMounted(() => loadTickets())
</script>

<template>
  <main class="workspace-page">
    <header class="page-heading">
      <div>
        <span class="section-kicker">{{ todayLabel }}</span>
        <h1>你好，{{ displayName }}</h1>
        <p>当前共有 {{ total }} 张你有权查看的工单，页面数据来自 FlowOps 后端。</p>
      </div>
      <el-button class="primary-action" type="primary" size="large" :icon="Plus" @click="showCreateHint">创建工单</el-button>
    </header>

    <el-alert v-if="pageError" class="workspace-alert" type="error" :title="pageError" show-icon :closable="false">
      <template #default>
        <el-button text type="danger" @click="loadTickets()">重新加载</el-button>
      </template>
    </el-alert>

    <section class="metrics-grid" aria-label="当前页工单概览">
      <article class="metric-card glass-card metric-card--mint">
        <div class="metric-card__icon"><el-icon><RefreshRight /></el-icon></div>
        <div class="metric-card__value">{{ total }}</div>
        <p>可见工单总数</p>
        <span class="metric-card__trend">依据当前用户数据范围</span>
      </article>
      <article class="metric-card glass-card metric-card--blue">
        <div class="metric-card__icon"><el-icon><Timer /></el-icon></div>
        <div class="metric-card__value">{{ waitingTickets.length }}</div>
        <p>本页等待处理</p>
        <span class="metric-card__trend">待分派、待接单或待确认</span>
      </article>
      <article class="metric-card glass-card metric-card--yellow">
        <div class="metric-card__icon"><el-icon><Warning /></el-icon></div>
        <div class="metric-card__value">{{ breachedTickets.length }}</div>
        <p>本页 SLA 超时</p>
        <span class="metric-card__trend">来自工单 SLA 快照</span>
      </article>
      <article class="metric-card glass-card metric-card--sage">
        <div class="metric-card__icon"><el-icon><CircleCheck /></el-icon></div>
        <div class="metric-card__value">{{ healthPercentage }}<span>%</span></div>
        <p>本页 SLA 健康度</p>
        <span class="metric-card__trend">按当前页数据计算</span>
      </article>
    </section>

    <section class="bento-grid">
      <article class="ticket-panel glass-card">
        <div class="panel-heading">
          <div>
            <span class="section-kicker">TICKET STREAM</span>
            <h2>真实工单数据</h2>
          </div>
          <span v-if="!pageError" class="connection-state">已连接后端</span>
        </div>

        <form class="ticket-toolbar" @submit.prevent="loadTickets(true)">
          <el-input v-model="keyword" class="ticket-search" placeholder="搜索标题" :prefix-icon="Search" clearable />
          <el-select v-model="selectedStatus" aria-label="工单状态" placeholder="全部状态" clearable>
            <el-option v-for="status in statusOptions" :key="status.value" :label="status.label" :value="status.value" />
          </el-select>
          <el-select v-model="selectedPriority" aria-label="工单优先级" placeholder="全部优先级" clearable>
            <el-option v-for="priority in ['P0', 'P1', 'P2']" :key="priority" :label="priority" :value="priority" />
          </el-select>
          <el-button class="ticket-toolbar__submit" type="primary" native-type="submit" :icon="Search" :loading="loading">查询</el-button>
        </form>

        <el-table v-loading="loading" class="ticket-table" :data="tickets" empty-text="没有符合条件的工单" @row-click="handleRowClick">
          <el-table-column label="工单" min-width="280">
            <template #default="scope">
              <div class="ticket-title-cell">
                <strong>{{ scope.row.title }}</strong>
                <span>{{ scope.row.ticketNo }}</span>
              </div>
            </template>
          </el-table-column>
          <el-table-column label="优先级" width="90">
            <template #default="scope">
              <span :class="priorityClass(scope.row.priority)">{{ scope.row.priority }}</span>
            </template>
          </el-table-column>
          <el-table-column label="状态" width="116">
            <template #default="scope">
              <span :class="statusClass(scope.row.status)">{{ statusLabels[scope.row.status as TicketStatus] }}</span>
            </template>
          </el-table-column>
          <el-table-column label="处理人" width="112">
            <template #default="scope">{{ assigneeLabel(scope.row.currentAssigneeId) }}</template>
          </el-table-column>
          <el-table-column label="最近更新" width="110">
            <template #default="scope">{{ relativeTime(scope.row.updatedAt) }}</template>
          </el-table-column>
          <el-table-column width="52" align="right">
            <template #default="scope">
              <button class="row-action" type="button" aria-label="查看工单" @click.stop="handleRowClick(scope.row)"><el-icon><ArrowRight /></el-icon></button>
            </template>
          </el-table-column>
        </el-table>

        <div class="table-footer">
          <span>本页 {{ tickets.length }} 张，共 {{ total }} 张</span>
          <el-pagination
            small
            background
            layout="prev, pager, next"
            :current-page="pageNum"
            :total="total"
            :page-size="pageSize"
            @current-change="handlePageChange"
          />
        </div>
      </article>

      <aside class="side-bento">
        <article class="sla-card glass-card">
          <div class="panel-heading panel-heading--compact">
            <div>
              <span class="section-kicker">SLA HEALTH</span>
              <h2>当前页健康度</h2>
            </div>
            <div class="metric-card__icon"><el-icon><TrendCharts /></el-icon></div>
          </div>

          <div class="sla-ring">
            <el-progress type="circle" :percentage="healthPercentage" :width="126" :stroke-width="10" color="#78bfa0">
              <template #default="{ percentage }">
                <strong>{{ percentage }}%</strong>
                <span>健康</span>
              </template>
            </el-progress>
          </div>

          <div class="sla-list">
            <div><span><i class="legend-dot legend-dot--mint"></i>未超时</span><strong>{{ tickets.length - breachedTickets.length }}</strong></div>
            <div><span><i class="legend-dot legend-dot--yellow"></i>监测中</span><strong>{{ pendingSlaTickets.length }}</strong></div>
            <div><span><i class="legend-dot legend-dot--rose"></i>已超时</span><strong>{{ breachedTickets.length }}</strong></div>
          </div>
        </article>

        <article class="focus-card glass-card">
          <span class="focus-card__label">CURRENT FOCUS</span>
          <h3>{{ urgentTicket ? '发现 P0 紧急工单' : '当前没有 P0 紧急项' }}</h3>
          <p v-if="urgentTicket">{{ urgentTicket.ticketNo }}：{{ urgentTicket.title }}</p>
          <p v-else>当前页没有需要立即处理的 P0 工单，可以继续关注待接单和待确认事项。</p>
          <button v-if="urgentTicket" class="focus-card__action" type="button" @click="showDetailHint(urgentTicket)">立即查看 <el-icon><ArrowRight /></el-icon></button>
        </article>
      </aside>
    </section>

    <p class="preview-note">当前已接入真实登录与工单分页；创建、详情和状态操作将在后续纵向闭环中逐步接入。</p>
  </main>
</template>
