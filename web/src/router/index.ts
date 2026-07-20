import { createRouter, createWebHistory } from 'vue-router'
import { ACCESS_TOKEN_KEY } from '@/constants/auth'
import { useAuthStore } from '@/stores/auth'
import { pinia } from '@/stores'
import LoginView from '@/views/LoginView.vue'
import AppLayout from '@/layouts/AppLayout.vue'
import TicketWorkspaceView from '@/views/TicketWorkspaceView.vue'

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
      path: '/',
      component: AppLayout,
      children: [
        { path: '', redirect: '/tickets' },
        { path: 'tickets', name: 'tickets', component: TicketWorkspaceView },
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
      if (to.name === 'login') {
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
