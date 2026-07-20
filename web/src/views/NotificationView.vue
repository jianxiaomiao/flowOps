<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { fetchNotificationPage, markNotificationRead } from '@/services/notification'
import type { NotificationItem } from '@/types/api'

const rows = ref<NotificationItem[]>([])
const total = ref(0)
const pageNum = ref(1)
const unreadOnly = ref(false)
const loading = ref(false)
async function load() { loading.value = true; try { const page = await fetchNotificationPage(pageNum.value, 10, unreadOnly.value); rows.value = page.records; total.value = page.total } finally { loading.value = false } }
async function markRead(row: unknown) { const item = row as NotificationItem; await markNotificationRead(item.id); ElMessage.success('已标记为已读'); await load() }
onMounted(load)
</script>
<template><main class="workspace-page"><header class="page-heading"><div><span class="section-kicker">INBOX</span><h1>消息通知</h1><p>来自 Outbox 与 RabbitMQ 消费链路的站内通知。</p></div><el-switch v-model="unreadOnly" active-text="只看未读" @change="load" /></header>
  <section class="glass-card module-card"><el-table v-loading="loading" :data="rows"><el-table-column label="状态" width="80"><template #default="scope"><el-tag :type="scope.row.readAt ? 'info' : 'success'">{{ scope.row.readAt ? '已读' : '未读' }}</el-tag></template></el-table-column><el-table-column prop="title" label="标题" width="220" /><el-table-column prop="content" label="内容" min-width="300" /><el-table-column prop="createdAt" label="时间" width="180" /><el-table-column label="操作" width="100"><template #default="scope"><el-button v-if="!scope.row.readAt" link type="primary" @click="markRead(scope.row)">标为已读</el-button></template></el-table-column></el-table><div class="table-footer"><span>共 {{ total }} 条</span><el-pagination v-model:current-page="pageNum" background layout="prev,pager,next" :total="total" :page-size="10" @current-change="load" /></div></section></main></template>
