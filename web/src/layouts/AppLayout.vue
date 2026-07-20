<script setup lang="ts">
import { computed, ref } from 'vue'
import { useRouter } from 'vue-router'
import {
  Bell,
  Document,
  Fold,
  OfficeBuilding,
  Setting,
  SwitchButton,
  UserFilled,
} from '@element-plus/icons-vue'
import { useAuthStore } from '@/stores/auth'

const router = useRouter()
const authStore = useAuthStore()
const sidebarCollapsed = ref(false)
const displayName = computed(() => authStore.username || '当前用户')
const avatarText = computed(() => displayName.value.slice(0, 1).toUpperCase())
const canManageUsers = computed(() => authStore.hasAuthority('user:manage'))
const canManageRoles = computed(() => authStore.hasAuthority('role:manage'))
const canManageSla = computed(() => authStore.hasAuthority('sla:manage'))
const canAccessOrganization = computed(() => canManageUsers.value || canManageRoles.value || canManageSla.value)

function logout() {
  authStore.logout()
  router.replace('/login')
}
</script>

<template>
  <div class="app-shell app-background" :class="{ 'app-shell--collapsed': sidebarCollapsed }">
    <aside class="app-sidebar glass-card">
      <div class="sidebar-brand">
        <span class="brand-mark">F</span>
        <span class="sidebar-brand__name">FlowOps</span>
      </div>

      <nav class="sidebar-nav" aria-label="主导航">
        <span class="sidebar-nav__label">工作空间</span>
        <RouterLink class="nav-item nav-item--active" to="/tickets">
          <el-icon><Document /></el-icon>
          <span>工单中心</span>
          <span class="nav-count">12</span>
        </RouterLink>
        <button v-if="canManageUsers" class="nav-item" type="button">
          <el-icon><Bell /></el-icon>
          <span>消息通知</span>
          <span class="nav-dot" aria-label="有新消息"></span>
        </button>

        <span v-if="canAccessOrganization" class="sidebar-nav__label">组织管理</span>
        <button v-if="canManageRoles" class="nav-item" type="button">
          <el-icon><OfficeBuilding /></el-icon>
          <span>部门与成员</span>
        </button>
        <button v-if="canManageSla" class="nav-item" type="button">
          <el-icon><UserFilled /></el-icon>
          <span>角色权限</span>
        </button>
        <button class="nav-item" type="button">
          <el-icon><Setting /></el-icon>
          <span>SLA 策略</span>
        </button>
      </nav>

      <div class="sidebar-footer">
        <button class="sidebar-user" type="button">
          <span class="user-avatar">{{ avatarText }}</span>
          <span class="sidebar-user__copy">
            <strong>{{ displayName }}</strong>
            <small>{{ authStore.tenantCode || '当前企业' }}</small>
          </span>
        </button>
        <button class="icon-button" type="button" aria-label="退出登录" @click="logout">
          <el-icon><SwitchButton /></el-icon>
        </button>
      </div>
    </aside>

    <section class="app-workspace">
      <header class="topbar glass-card">
        <button class="icon-button sidebar-toggle" type="button" aria-label="切换侧栏" @click="sidebarCollapsed = !sidebarCollapsed">
          <el-icon><Fold /></el-icon>
        </button>
        <div class="topbar__trail">
          <span>工作空间</span>
          <span>/</span>
          <strong>工单中心</strong>
        </div>
        <div class="topbar__actions">
          <span class="sync-status"><i></i>服务运行正常</span>
          <button class="notification-button" type="button" aria-label="查看通知">
            <el-icon><Bell /></el-icon>
            <span></span>
          </button>
        </div>
      </header>

      <RouterView />
    </section>
  </div>
</template>
