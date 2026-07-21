<script setup lang="ts">
import { computed, onMounted, onUnmounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { Bell, Collection, Document, Fold, OfficeBuilding, Setting, SwitchButton, UserFilled } from '@element-plus/icons-vue'
import { useAuthStore } from '@/stores/auth'
import { fetchUnreadNotificationCount } from '@/services/notification'

const route = useRoute(); const router = useRouter(); const auth = useAuthStore(); const sidebarCollapsed = ref(false)
const displayName = computed(() => auth.username || '当前用户')
const avatarText = computed(() => displayName.value.slice(0, 1).toUpperCase())
const pageTitle = computed(() => typeof route.meta.title === 'string' ? route.meta.title : '工作空间')
const canManageUsers = computed(() => auth.hasAuthority('user:manage'))
const canManageRoles = computed(() => auth.hasAuthority('role:manage'))
const canManageSla = computed(() => auth.hasAuthority('sla:manage'))
const canManageTenants = computed(() => auth.hasAuthority('platform:tenant:manage'))
const canReadKnowledge = computed(() => auth.hasAuthority('knowledge:read') || auth.hasAuthority('knowledge:write') || auth.hasAuthority('knowledge:manage'))
const hasOrganizationMenu = computed(() => canManageUsers.value || canManageRoles.value || canManageSla.value)
const unreadCount = ref(0)
let unreadTimer: number | undefined
async function loadUnreadCount() { try { unreadCount.value = await fetchUnreadNotificationCount() } catch { /* 统一拦截器负责错误处理 */ } }
async function logout() { await auth.logoutFromServer(); await router.replace('/login') }
onMounted(() => { loadUnreadCount(); window.addEventListener('flowops:notification-changed', loadUnreadCount); unreadTimer = window.setInterval(loadUnreadCount, 30_000) })
onUnmounted(() => { window.removeEventListener('flowops:notification-changed', loadUnreadCount); if (unreadTimer) window.clearInterval(unreadTimer) })
</script>
<template><div class="app-shell app-background" :class="{ 'app-shell--collapsed': sidebarCollapsed }"><aside class="app-sidebar glass-card"><div class="sidebar-brand"><span class="brand-mark">F</span><span class="sidebar-brand__name">FlowOps</span></div><nav class="sidebar-nav" aria-label="主导航">
  <span class="sidebar-nav__label">工作空间</span><RouterLink class="nav-item" active-class="nav-item--active" to="/tickets"><el-icon><Document /></el-icon><span>工单中心</span></RouterLink><RouterLink class="nav-item" active-class="nav-item--active" to="/notifications"><el-icon><Bell /></el-icon><span>消息通知</span></RouterLink><RouterLink v-if="canReadKnowledge" class="nav-item" active-class="nav-item--active" to="/knowledge"><el-icon><Collection /></el-icon><span>知识库</span></RouterLink>
  <span v-if="hasOrganizationMenu" class="sidebar-nav__label">组织管理</span><RouterLink v-if="canManageUsers" class="nav-item" active-class="nav-item--active" to="/organization"><el-icon><OfficeBuilding /></el-icon><span>部门与成员</span></RouterLink><RouterLink v-if="canManageRoles" class="nav-item" active-class="nav-item--active" to="/rbac"><el-icon><UserFilled /></el-icon><span>角色权限</span></RouterLink><RouterLink v-if="canManageSla" class="nav-item" active-class="nav-item--active" to="/sla"><el-icon><Setting /></el-icon><span>SLA 策略</span></RouterLink>
  <span v-if="canManageTenants" class="sidebar-nav__label">平台管理</span><RouterLink v-if="canManageTenants" class="nav-item" active-class="nav-item--active" to="/tenants"><el-icon><OfficeBuilding /></el-icon><span>租户管理</span></RouterLink></nav>
  <div class="sidebar-footer"><button class="sidebar-user" type="button"><span class="user-avatar">{{ avatarText }}</span><span class="sidebar-user__copy"><strong>{{ displayName }}</strong><small>{{ auth.tenantCode || `租户 #${auth.tenantId ?? '-'}` }}</small></span></button><button class="icon-button" type="button" aria-label="退出登录" @click="logout"><el-icon><SwitchButton /></el-icon></button></div></aside>
  <section class="app-workspace"><header class="topbar glass-card"><button class="icon-button sidebar-toggle" type="button" aria-label="切换侧栏" @click="sidebarCollapsed=!sidebarCollapsed"><el-icon><Fold /></el-icon></button><div class="topbar__trail"><span>工作空间</span><span>/</span><strong>{{ pageTitle }}</strong></div><div class="topbar__actions"><span class="sync-status"><i></i>服务运行正常</span><RouterLink class="notification-button" to="/notifications" aria-label="查看通知"><el-badge :value="unreadCount" :hidden="unreadCount === 0" :max="99"><el-icon><Bell /></el-icon></el-badge></RouterLink></div></header><RouterView /></section></div></template>
