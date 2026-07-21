<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Plus, Search } from '@element-plus/icons-vue'
import { useAuthStore } from '@/stores/auth'
import { fetchDepartments, fetchUserPage } from '@/services/organization'
import {
  addTicketComment, assignTicket, createTicket, fetchTicket, fetchTicketAssignments,
  fetchTicketComments, fetchTicketKnowledgeArticles, fetchTicketPage, fetchTicketTransitions, transitionTicket,
} from '@/services/ticket'
import { fetchKnowledgePage, linkKnowledgeArticleToTicket } from '@/services/knowledge'
import type { Department, KnowledgeArticle, TicketAction, TicketAssignmentRecord, TicketComment, TicketPreview, TicketStatus, TicketTransitionRecord, UserAccount } from '@/types/api'

const auth = useAuthStore()
const tickets = ref<TicketPreview[]>([])
const departments = ref<Department[]>([])
const users = ref<UserAccount[]>([])
const total = ref(0)
const pageNum = ref(1)
const pageSize = ref(10)
const loading = ref(false)
const keyword = ref('')
const status = ref<TicketStatus | ''>('')
const priority = ref<'' | 'P0' | 'P1' | 'P2'>('')
const createVisible = ref(false)
const detailVisible = ref(false)
const assignmentVisible = ref(false)
const commentVisible = ref(false)
const selected = ref<TicketPreview | null>(null)
const transitions = ref<TicketTransitionRecord[]>([])
const assignments = ref<TicketAssignmentRecord[]>([])
const comments = ref<TicketComment[]>([])
const knowledgeArticles = ref<KnowledgeArticle[]>([])
const knowledgeLinkVisible = ref(false)
const knowledgeKeyword = ref('')
const knowledgeCandidates = ref<KnowledgeArticle[]>([])

const createForm = reactive({ handlingDepartmentId: undefined as number | undefined, title: '', description: '', ticketType: 'INCIDENT', priority: 'P2' as 'P0' | 'P1' | 'P2' })
const assignmentForm = reactive({ toDepartmentId: undefined as number | undefined, toAssigneeId: undefined as number | undefined, reason: '' })
const commentForm = reactive({ content: '', visibility: 'PUBLIC' as 'PUBLIC' | 'INTERNAL' })

const statusLabels: Record<TicketStatus, string> = { OPEN: '待分派', ASSIGNED: '待接单', ACCEPTED: '已接单', PROCESSING: '处理中', PENDING_CONFIRMATION: '待确认', RESOLVED: '已解决', CLOSED: '已关闭' }
const actionLabels: Record<TicketAction, string> = { ACCEPT: '接单', START_PROCESS: '开始处理', SUBMIT_RESOLUTION: '提交方案', CONFIRM: '确认解决', REJECT: '驳回方案', CLOSE: '关闭工单' }
const actionByStatus: Partial<Record<TicketStatus, TicketAction[]>> = { ASSIGNED: ['ACCEPT'], ACCEPTED: ['START_PROCESS'], PROCESSING: ['SUBMIT_RESOLUTION'], PENDING_CONFIRMATION: ['CONFIRM', 'REJECT'], RESOLVED: ['CLOSE'] }
const actionAuthority: Record<TicketAction, string> = { ACCEPT: 'ticket:accept', START_PROCESS: 'ticket:update', SUBMIT_RESOLUTION: 'ticket:resolve', CONFIRM: 'ticket:confirm', REJECT: 'ticket:confirm', CLOSE: 'ticket:confirm' }
const availableActions = computed(() => selected.value ? (actionByStatus[selected.value.status] ?? []).filter((action) => auth.hasAuthority(actionAuthority[action])) : [])

async function loadTickets() {
  loading.value = true
  try {
    const page = await fetchTicketPage({ pageNum: pageNum.value, pageSize: pageSize.value, title: keyword.value || undefined, status: status.value || undefined, priority: priority.value || undefined })
    tickets.value = page.records; total.value = page.total
  } finally { loading.value = false }
}

async function loadOptions() {
  if (auth.hasAuthority('user:manage')) {
    const [departmentList, userPage] = await Promise.all([fetchDepartments(), fetchUserPage({ pageNum: 1, pageSize: 100, status: 'ACTIVE' })])
    departments.value = departmentList.filter((item) => item.status === 'ACTIVE'); users.value = userPage.records
  }
}

async function submitCreate() {
  if (!createForm.handlingDepartmentId || !createForm.title || !createForm.description) return ElMessage.warning('请填写完整工单信息')
  await createTicket({ ...createForm, handlingDepartmentId: createForm.handlingDepartmentId })
  createVisible.value = false; ElMessage.success('工单创建成功'); await loadTickets()
}

async function openDetail(row: TicketPreview) {
  detailVisible.value = true
  selected.value = await fetchTicket(row.id)
  const [transitionRows, assignmentRows, commentRows, linkedArticles] = await Promise.all([fetchTicketTransitions(row.id), fetchTicketAssignments(row.id), fetchTicketComments(row.id), fetchTicketKnowledgeArticles(row.id)])
  transitions.value = transitionRows; assignments.value = assignmentRows; comments.value = commentRows; knowledgeArticles.value = linkedArticles
}

async function submitAssignment() {
  if (!selected.value || !assignmentForm.toDepartmentId) return ElMessage.warning('请选择目标部门')
  selected.value = await assignTicket(selected.value.id, { toDepartmentId: assignmentForm.toDepartmentId, toAssigneeId: assignmentForm.toAssigneeId, reason: assignmentForm.reason || undefined, version: selected.value.version })
  assignmentVisible.value = false; ElMessage.success('分派成功'); await openDetail(selected.value); await loadTickets()
}

async function executeAction(action: TicketAction) {
  if (!selected.value) return
  let value = ''
  if (action === 'SUBMIT_RESOLUTION') {
    const result = await ElMessageBox.prompt('请输入解决方案', actionLabels[action], { inputType: 'textarea', inputValidator: (text) => Boolean(text) || '解决方案不能为空' })
    value = result.value
  } else {
    const result = await ElMessageBox.prompt('可填写操作备注', actionLabels[action], { inputType: 'textarea', inputPlaceholder: '选填' })
    value = result.value
  }
  selected.value = await transitionTicket(selected.value.id, { action, version: selected.value.version, remark: action === 'SUBMIT_RESOLUTION' ? undefined : value || undefined, resolution: action === 'SUBMIT_RESOLUTION' ? value : undefined })
  ElMessage.success(`${actionLabels[action]}成功`); await openDetail(selected.value); await loadTickets()
}

async function submitComment() {
  if (!selected.value || !commentForm.content.trim()) return ElMessage.warning('评论不能为空')
  await addTicketComment(selected.value.id, { content: commentForm.content, visibility: commentForm.visibility })
  commentVisible.value = false; commentForm.content = ''; ElMessage.success('评论已添加'); await openDetail(selected.value)
}

async function searchKnowledge() { const page = await fetchKnowledgePage({ pageNum: 1, pageSize: 10, keyword: knowledgeKeyword.value || undefined }); knowledgeCandidates.value = page.records }
async function openKnowledgeLink() { knowledgeLinkVisible.value = true; await searchKnowledge() }
async function linkKnowledge(value: unknown) { const article = value as KnowledgeArticle; if (!selected.value) return; await linkKnowledgeArticleToTicket(article.id, selected.value.id); ElMessage.success('知识文章已关联'); knowledgeArticles.value = await fetchTicketKnowledgeArticles(selected.value.id); knowledgeLinkVisible.value = false }

onMounted(async () => { await Promise.all([loadTickets(), loadOptions()]) })
</script>

<template>
  <main class="workspace-page">
    <header class="page-heading">
      <div><span class="section-kicker">TICKET CENTER</span><h1>工单中心</h1><p>创建、分派、处理和确认都已连接真实后端状态机。</p></div>
      <el-button v-if="auth.hasAuthority('ticket:create')" class="primary-action" type="primary" :icon="Plus" @click="createVisible = true">创建工单</el-button>
    </header>
    <section class="glass-card module-card">
      <form class="ticket-toolbar" @submit.prevent="loadTickets">
        <el-input v-model="keyword" class="ticket-search" placeholder="搜索标题" :prefix-icon="Search" clearable />
        <el-select v-model="status" placeholder="全部状态" clearable><el-option v-for="(label, value) in statusLabels" :key="value" :label="label" :value="value" /></el-select>
        <el-select v-model="priority" placeholder="全部优先级" clearable><el-option v-for="item in ['P0','P1','P2']" :key="item" :label="item" :value="item" /></el-select>
        <el-button type="primary" native-type="submit" :icon="Search">查询</el-button>
      </form>
      <el-table v-loading="loading" class="ticket-table" :data="tickets" @row-click="openDetail">
        <el-table-column prop="ticketNo" label="工单号" width="170" /><el-table-column prop="title" label="标题" min-width="220" />
        <el-table-column prop="priority" label="优先级" width="90" /><el-table-column label="状态" width="110"><template #default="scope">{{ statusLabels[scope.row.status as TicketStatus] }}</template></el-table-column>
        <el-table-column prop="currentAssigneeId" label="处理人 ID" width="110" /><el-table-column prop="updatedAt" label="更新时间" width="170" />
      </el-table>
      <div class="table-footer"><span>共 {{ total }} 张</span><el-pagination background layout="prev,pager,next" :total="total" :page-size="pageSize" v-model:current-page="pageNum" @current-change="loadTickets" /></div>
    </section>

    <el-dialog v-model="createVisible" title="创建工单" width="560px">
      <el-form label-position="top"><el-form-item label="处理部门"><el-select v-model="createForm.handlingDepartmentId" filterable><el-option v-for="item in departments" :key="item.id" :label="item.departmentName" :value="item.id" /></el-select></el-form-item>
        <el-form-item label="标题"><el-input v-model="createForm.title" /></el-form-item><el-form-item label="描述"><el-input v-model="createForm.description" type="textarea" :rows="4" /></el-form-item>
        <div class="form-grid"><el-form-item label="工单类型"><el-input v-model="createForm.ticketType" /></el-form-item><el-form-item label="优先级"><el-select v-model="createForm.priority"><el-option v-for="item in ['P0','P1','P2']" :key="item" :value="item" /></el-select></el-form-item></div>
      </el-form><template #footer><el-button @click="createVisible=false">取消</el-button><el-button type="primary" @click="submitCreate">创建</el-button></template>
    </el-dialog>

    <el-drawer v-model="detailVisible" size="720px" :title="selected ? `${selected.ticketNo} · ${selected.title}` : '工单详情'">
      <template v-if="selected"><el-descriptions :column="2" border><el-descriptions-item label="状态">{{ statusLabels[selected.status] }}</el-descriptions-item><el-descriptions-item label="版本">{{ selected.version }}</el-descriptions-item><el-descriptions-item label="优先级">{{ selected.priority }}</el-descriptions-item><el-descriptions-item label="处理人">{{ selected.currentAssigneeId ?? '未分派' }}</el-descriptions-item><el-descriptions-item label="描述" :span="2">{{ selected.description }}</el-descriptions-item><el-descriptions-item v-if="selected.resolution" label="解决方案" :span="2">{{ selected.resolution }}</el-descriptions-item></el-descriptions>
        <div class="detail-actions"><el-button v-if="auth.hasAuthority('ticket:assign')" @click="assignmentVisible=true">分派</el-button><el-button v-for="action in availableActions" :key="action" type="primary" @click="executeAction(action)">{{ actionLabels[action] }}</el-button><el-button v-if="auth.hasAuthority('ticket:update')" @click="commentVisible=true">添加评论</el-button></div>
        <el-tabs class="detail-tabs"><el-tab-pane label="状态历史"><el-timeline><el-timeline-item v-for="item in transitions" :key="item.id" :timestamp="item.createdAt">{{ item.fromStatus }} → {{ item.toStatus }} · {{ item.action }}<p>{{ item.remark }}</p></el-timeline-item></el-timeline></el-tab-pane>
          <el-tab-pane label="分派历史"><el-timeline><el-timeline-item v-for="item in assignments" :key="item.id" :timestamp="item.createdAt">部门 {{ item.fromDepartmentId ?? '-' }} → {{ item.toDepartmentId }}，处理人 {{ item.toAssigneeId ?? '未指定' }}</el-timeline-item></el-timeline></el-tab-pane>
          <el-tab-pane label="评论"><div v-for="item in comments" :key="item.id" class="comment-item"><strong>用户 #{{ item.authorId }} · {{ item.visibility }}</strong><p>{{ item.content }}</p><small>{{ item.createdAt }}</small></div></el-tab-pane><el-tab-pane label="知识关联"><el-button v-if="auth.hasAuthority('knowledge:write') || auth.hasAuthority('knowledge:manage')" @click="openKnowledgeLink">关联知识</el-button><div v-for="item in knowledgeArticles" :key="item.id" class="comment-item"><strong>{{ item.title }}</strong><p>{{ item.summary || item.content }}</p><small>{{ item.visibility }} · {{ item.updatedAt }}</small></div></el-tab-pane></el-tabs>
      </template>
    </el-drawer>

    <el-dialog v-model="assignmentVisible" title="分派工单" width="480px"><el-form label-position="top"><el-form-item label="目标部门"><el-select v-model="assignmentForm.toDepartmentId"><el-option v-for="item in departments" :key="item.id" :label="item.departmentName" :value="item.id" /></el-select></el-form-item><el-form-item label="目标处理人（可选）"><el-select v-model="assignmentForm.toAssigneeId" clearable><el-option v-for="item in users" :key="item.id" :label="item.displayName" :value="item.id" /></el-select></el-form-item><el-form-item label="原因"><el-input v-model="assignmentForm.reason" type="textarea" /></el-form-item></el-form><template #footer><el-button @click="assignmentVisible=false">取消</el-button><el-button type="primary" @click="submitAssignment">确认分派</el-button></template></el-dialog>
    <el-dialog v-model="commentVisible" title="添加评论" width="480px"><el-form label-position="top"><el-form-item label="可见性"><el-radio-group v-model="commentForm.visibility"><el-radio value="PUBLIC">公开</el-radio><el-radio value="INTERNAL">内部</el-radio></el-radio-group></el-form-item><el-form-item label="内容"><el-input v-model="commentForm.content" type="textarea" :rows="4" /></el-form-item></el-form><template #footer><el-button @click="commentVisible=false">取消</el-button><el-button type="primary" @click="submitComment">提交</el-button></template></el-dialog>
    <el-dialog v-model="knowledgeLinkVisible" title="关联知识文章" width="620px"><form class="ticket-toolbar" @submit.prevent="searchKnowledge"><el-input v-model="knowledgeKeyword" placeholder="搜索可见知识" clearable /><el-button type="primary" native-type="submit">检索</el-button></form><el-table :data="knowledgeCandidates"><el-table-column prop="title" label="标题" /><el-table-column prop="summary" label="摘要" /><el-table-column label="操作" width="90"><template #default="scope"><el-button link type="primary" @click="linkKnowledge(scope.row)">关联</el-button></template></el-table-column></el-table></el-dialog>
  </main>
</template>
