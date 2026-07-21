import { createRouter, createWebHistory } from 'vue-router'
import { ACCESS_TOKEN_KEY } from '@/constants/auth'
import { useAuthStore } from '@/stores/auth'
import { pinia } from '@/stores'

const LoginView = () => import('@/views/LoginView.vue')
const RegisterView = () => import('@/views/RegisterView.vue')
const AppLayout = () => import('@/layouts/AppLayout.vue')

const router = createRouter({
  history: createWebHistory(),
  routes: [
    {
      path: '/login',
      name: 'login',
      component: LoginView,
      meta: { public: true },
    },
    {
      path: '/register',
      name: 'register',
      component: RegisterView,
      meta: { public: true },
    },
    {
      path: '/',
      component: AppLayout,
      children: [
        { path: '', redirect: '/tickets' },
        { path: 'tickets', name: 'tickets', component: () => import('@/views/TicketWorkspaceView.vue'), meta: { title: '工单中心' } },
        { path: 'notifications', name: 'notifications', component: () => import('@/views/NotificationView.vue'), meta: { title: '消息通知' } },
        { path: 'organization', name: 'organization', component: () => import('@/views/OrganizationView.vue'), meta: { title: '部门与成员', authority: 'user:manage' } },
        { path: 'rbac', name: 'rbac', component: () => import('@/views/RbacView.vue'), meta: { title: '角色权限', authority: 'role:manage' } },
        { path: 'sla', name: 'sla', component: () => import('@/views/SlaPolicyView.vue'), meta: { title: 'SLA 策略', authority: 'sla:manage' } },
        { path: 'tenants', name: 'tenants', component: () => import('@/views/TenantView.vue'), meta: { title: '租户管理', authority: 'platform:tenant:manage' } },
      ],
    },
    { path: '/:pathMatch(.*)*', redirect: '/tickets' },
  ],
})

router.beforeEach(async (to) => {
  const hasToken = Boolean(sessionStorage.getItem(ACCESS_TOKEN_KEY))
  if (!to.meta.public && !hasToken) {
    return { name: 'login', query: { redirect: to.fullPath } }
  }

  if (hasToken) {
    const authStore = useAuthStore(pinia)
    try {
      await authStore.loadCurrentUser()
      const requiredAuthority = to.meta.authority
      if (typeof requiredAuthority === 'string' && !authStore.hasAuthority(requiredAuthority)) {
        return { name: 'tickets' }
      }
      if (to.name === 'login' || to.name === 'register') {
        return { name: 'tickets' }
      }
    } catch {
      if (!to.meta.public) {
        return { name: 'login', query: { redirect: to.fullPath } }
      }
    }
  }
  return true
})

export default router
