<script setup lang="ts">
import { computed, ref } from 'vue'
import {
  ArrowRight,
  Bell,
  Check,
  Collection,
  Document,
  Lock,
  MagicStick,
  Monitor,
  Timer,
} from '@element-plus/icons-vue'
import { useAuthStore } from '@/stores/auth'

interface PreviewStage {
  key: 'OPEN' | 'PROCESSING' | 'PENDING_CONFIRMATION' | 'RESOLVED'
  label: string
  tagType: 'info' | 'warning' | 'primary' | 'success'
  version: string
  owner: string
  slaHint: string
  summary: string
  actionHint: string
}

const authStore = useAuthStore()

const previewStages: PreviewStage[] = [
  {
    key: 'OPEN',
    label: '待分派',
    tagType: 'info',
    version: 'v1',
    owner: '待分派 · 平台运维组',
    slaHint: '响应截止快照：剩余 28 分钟',
    summary: '财务报表导出任务在高峰期出现超时，影响华东区结算核对。',
    actionHint: '按部门与权限自动分派处理人，建单即锁定 SLA 承诺快照',
  },
  {
    key: 'PROCESSING',
    label: '处理中',
    tagType: 'warning',
    version: 'v3',
    owner: '林晨 · 数据平台组',
    slaHint: '解决截止快照：剩余 3 小时 12 分',
    summary: '已定位为慢查询锁竞争，正在按知识库索引优化方案执行验证。',
    actionHint: '写操作携带乐观锁版本，多人并发更新返回 409 防止静默覆盖',
  },
  {
    key: 'PENDING_CONFIRMATION',
    label: '待确认',
    tagType: 'primary',
    version: 'v4',
    owner: '等待提交人确认',
    slaHint: '方案已提交 · 通知已投递',
    summary: '已补充复合索引并拆分导出批次，验证耗时从 42s 降至 2.1s。',
    actionHint: '提交人可一键确认关闭，或驳回退回处理中继续跟进',
  },
  {
    key: 'RESOLVED',
    label: '已解决',
    tagType: 'success',
    version: 'v5',
    owner: '闭环归档 · 知识已关联',
    slaHint: 'SLA 达标 · 全链路已审计',
    summary: '问题已确认解决，处理方案同步沉淀至部门知识库供后续检索。',
    actionHint: '完整保留状态流转、评论、分派历史与 TraceId 审计记录',
  },
]

const workflowSteps = [
  'OPEN',
  'ASSIGNED',
  'ACCEPTED',
  'PROCESSING',
  'PENDING_CONFIRMATION',
  'RESOLVED',
  'CLOSED',
]

const coreLoopCards = [
  {
    index: '01',
    title: '受控工单流转',
    description: '从提交、分派、接单、处理到确认关闭，状态只能通过明确业务动作推进，杜绝绕过规则直接改写。',
    tone: 'sage',
  },
  {
    index: '02',
    title: '知识沉淀与 AI 协同',
    description: '工单处理经验可沉淀为结构化知识；处理人按需触发相似知识推荐，自主决定采用或忽略。',
    tone: 'yellow',
  },
  {
    index: '03',
    title: 'SLA 承诺与即时通知',
    description: '创建工单时即固化响应与解决时限快照，配合站内通知与超时扫描，关键问题不错过。',
    tone: 'blue',
  },
]

const trustCards = [
  {
    icon: Lock,
    badge: '多租户与 RBAC',
    title: '企业数据严格隔离',
    description: '租户身份仅从验签后的安全上下文获取，叠加本人、部门与租户级数据范围控制，前后端双重守门。',
    tone: 'sage',
  },
  {
    icon: Timer,
    badge: 'SLA 快照 · 乐观锁',
    title: '承诺可追溯，并发不覆盖',
    description: '后续修改 SLA 策略不会篡改历史工单承诺；关键写操作校验版本号，并发冲突明确提示而非静默覆盖。',
    tone: 'yellow',
  },
  {
    icon: Bell,
    badge: 'Outbox · RabbitMQ',
    title: '异步通知高可靠投递',
    description: '业务事务与发件箱同事务落库，配合消息重试、死信队列与事件 ID 幂等去重，波动期间不丢提醒。',
    tone: 'blue',
  },
  {
    icon: Monitor,
    badge: 'AI 边界 · TraceId',
    title: '权限可控，全链路可排查',
    description: 'AI 向量召回后仍由业务库二次复核发布状态与可见范围；所有请求贯穿 TraceId 与脱敏审计日志。',
    tone: 'mint',
  },
]

const activeStageKey = ref<PreviewStage['key']>('PROCESSING')
const aiDecision = ref<'idle' | 'adopted' | 'dismissed'>('idle')

const activeStage = computed(
  () => previewStages.find((stage) => stage.key === activeStageKey.value) ?? previewStages[1],
)

const userSummary = computed(() => {
  if (!authStore.isAuthenticated) return ''
  if (authStore.tenantCode && authStore.username) {
    return `${authStore.tenantCode} · ${authStore.username}`
  }
  return authStore.username || '已登录'
})

function selectStage(key: PreviewStage['key']) {
  activeStageKey.value = key
}

function setAiDecision(decision: 'adopted' | 'dismissed') {
  aiDecision.value = aiDecision.value === decision ? 'idle' : decision
}
</script>

<template>
  <div class="landing-page app-background">
    <header class="landing-nav-wrap">
      <nav class="landing-nav glass-card" aria-label="首页导航">
        <RouterLink class="brand-lockup" to="/">
          <span class="brand-mark">F</span>
          <span>FlowOps</span>
        </RouterLink>

        <div class="landing-nav__links">
          <a href="#workflow">工作流</a>
          <a href="#capabilities">核心能力</a>
          <a href="#reliability">可靠保障</a>
        </div>

        <div class="landing-nav__actions">
          <template v-if="authStore.isAuthenticated">
            <span class="landing-user-pill">{{ userSummary }}</span>
            <RouterLink to="/tickets">
              <el-button type="primary">
                进入工作台
                <el-icon><ArrowRight /></el-icon>
              </el-button>
            </RouterLink>
          </template>
          <template v-else>
            <RouterLink to="/login">
              <el-button text>登录</el-button>
            </RouterLink>
            <RouterLink to="/register">
              <el-button type="primary">
                免费创建企业
                <el-icon><ArrowRight /></el-icon>
              </el-button>
            </RouterLink>
          </template>
        </div>
      </nav>
    </header>

    <main class="landing-main">
      <section class="landing-hero">
        <div class="landing-hero__copy">
          <span class="landing-eyebrow">多租户企业工单 · 知识沉淀 · 人工可控 AI 推荐</span>
          <h1 class="landing-hero__title">
            从问题提交到知识沉淀，<br />让内部协作形成可控闭环
          </h1>
          <p class="landing-hero__desc">
            FlowOps 把企业工单流转、SLA 服务承诺、站内消息通知与团队知识库收拢到一个轻盈的工作空间，兼顾开箱即用的协作体验与清晰可验证的工程边界。
          </p>

          <div class="landing-hero__actions">
            <RouterLink v-if="authStore.isAuthenticated" to="/tickets">
              <el-button type="primary" size="large">
                进入工单工作台
                <el-icon><ArrowRight /></el-icon>
              </el-button>
            </RouterLink>
            <template v-else>
              <RouterLink to="/register">
                <el-button type="primary" size="large">
                  创建企业工作空间
                  <el-icon><ArrowRight /></el-icon>
                </el-button>
              </RouterLink>
              <RouterLink to="/login">
                <el-button size="large">登录已有账号</el-button>
              </RouterLink>
            </template>
          </div>

          <ul class="landing-facts" aria-label="平台核心特性">
            <li>
              <el-icon><Document /></el-icon>
              <span>7 态受控工单状态机</span>
            </li>
            <li>
              <el-icon><Lock /></el-icon>
              <span>租户 + RBAC 双重隔离</span>
            </li>
            <li>
              <el-icon><Collection /></el-icon>
              <span>按需触发 AI 知识推荐</span>
            </li>
          </ul>
        </div>

        <aside class="landing-preview glass-card" aria-label="工单工作台交互预览">
          <div class="landing-preview__top">
            <div>
              <span class="section-kicker">LIVE WORKFLOW PREVIEW</span>
              <h2>FO-2048 · 高峰期报表导出超时排查</h2>
            </div>
            <div class="landing-preview__badges">
              <el-tag :type="activeStage.tagType" effect="light" round>{{ activeStage.label }}</el-tag>
              <span class="landing-version-chip">{{ activeStage.version }}</span>
            </div>
          </div>

          <div class="landing-stage-tabs" role="tablist" aria-label="切换工单状态预览">
            <button
              v-for="stage in previewStages"
              :key="stage.key"
              type="button"
              role="tab"
              class="landing-stage-tab"
              :class="{ 'landing-stage-tab--active': stage.key === activeStage.key }"
              :aria-selected="stage.key === activeStage.key"
              @click="selectStage(stage.key)"
            >
              {{ stage.label }}
            </button>
          </div>

          <div class="landing-preview__body">
            <div class="landing-preview__meta">
              <span>{{ activeStage.owner }}</span>
              <span class="landing-sla-pill">
                <el-icon><Timer /></el-icon>
                {{ activeStage.slaHint }}
              </span>
            </div>
            <p class="landing-preview__summary">{{ activeStage.summary }}</p>
            <p class="landing-preview__hint">{{ activeStage.actionHint }}</p>
          </div>

          <div class="landing-ai-card">
            <div class="landing-ai-card__head">
              <span class="landing-ai-card__title">
                <el-icon><MagicStick /></el-icon>
                AI 相似知识推荐（按需触发）
              </span>
              <span class="landing-ai-card__scope">租户召回 · 权限已复核</span>
            </div>
            <p class="landing-ai-card__doc">《大表分页导出慢查询与批次拆分排查手册》 · 匹配度 92%</p>
            <div class="landing-ai-card__actions">
              <el-button
                size="small"
                :type="aiDecision === 'adopted' ? 'primary' : 'default'"
                @click="setAiDecision('adopted')"
              >
                <el-icon><Check /></el-icon>
                {{ aiDecision === 'adopted' ? '已采用到处理方案' : '采用建议' }}
              </el-button>
              <el-button
                size="small"
                text
                @click="setAiDecision('dismissed')"
              >
                {{ aiDecision === 'dismissed' ? '已忽略（可撤销）' : '忽略' }}
              </el-button>
            </div>
          </div>
        </aside>
      </section>

      <section id="workflow" class="landing-section">
        <div class="landing-section__head">
          <span class="section-kicker">WORKFLOW LOOP</span>
          <h2>一条清晰、可审计的业务闭环</h2>
          <p>每个状态切换都对应明确的业务动作与责任人，不再依赖口头群聊和表格手工对账。</p>
        </div>

        <div class="landing-state-strip glass-card" aria-label="工单状态机流转链路">
          <div class="landing-state-strip__flow">
            <template v-for="(step, idx) in workflowSteps" :key="step">
              <span class="landing-state-node">{{ step }}</span>
              <span v-if="idx < workflowSteps.length - 1" class="landing-state-arrow" aria-hidden="true">→</span>
            </template>
          </div>
          <span class="landing-state-strip__note">支持待确认阶段 REJECT 退回 PROCESSING，所有流转自动写入操作历史</span>
        </div>

        <div id="capabilities" class="landing-bento-three">
          <article
            v-for="card in coreLoopCards"
            :key="card.index"
            class="landing-card glass-card"
            :class="`landing-card--${card.tone}`"
          >
            <span class="landing-card__index">{{ card.index }}</span>
            <h3>{{ card.title }}</h3>
            <p>{{ card.description }}</p>
          </article>
        </div>
      </section>

      <section id="reliability" class="landing-section">
        <div class="landing-section__head">
          <span class="section-kicker">ENGINEERING TRUST</span>
          <h2>轻量体验背后，是克制而扎实的工程边界</h2>
          <p>不只把界面做清爽，更把权限隔离、并发一致性、消息可靠性与 AI 安全边界落实在主链路里。</p>
        </div>

        <div class="landing-bento-four">
          <article
            v-for="item in trustCards"
            :key="item.title"
            class="landing-trust-card glass-card"
            :class="`landing-trust-card--${item.tone}`"
          >
            <div class="landing-trust-card__top">
              <span class="landing-trust-card__icon">
                <el-icon><component :is="item.icon" /></el-icon>
              </span>
              <span class="landing-trust-card__badge">{{ item.badge }}</span>
            </div>
            <h3>{{ item.title }}</h3>
            <p>{{ item.description }}</p>
          </article>
        </div>
      </section>

      <section class="landing-section">
        <div class="landing-cta glass-card">
          <div class="landing-cta__copy">
            <span class="section-kicker">GET STARTED</span>
            <h2>30 秒创建你的企业工作空间</h2>
            <p>自助注册将原子初始化企业租户、默认部门与首位管理员，立即体验工单流转、SLA 策略与知识库协作。</p>
          </div>
          <div class="landing-cta__actions">
            <RouterLink v-if="authStore.isAuthenticated" to="/tickets">
              <el-button type="primary" size="large">
                回到工作台
                <el-icon><ArrowRight /></el-icon>
              </el-button>
            </RouterLink>
            <template v-else>
              <RouterLink to="/register">
                <el-button type="primary" size="large">
                  立即创建企业
                  <el-icon><ArrowRight /></el-icon>
                </el-button>
              </RouterLink>
              <RouterLink to="/login">
                <el-button size="large">登录工作台</el-button>
              </RouterLink>
            </template>
          </div>
        </div>
      </section>
    </main>

    <footer class="landing-footer">
      <span>FlowOps · 企业内部工单与知识协作平台</span>
      <div class="landing-footer__links">
        <RouterLink to="/login">登录</RouterLink>
        <RouterLink to="/register">注册企业</RouterLink>
        <RouterLink to="/tickets">工作台</RouterLink>
      </div>
    </footer>
  </div>
</template>
