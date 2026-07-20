import { createPinia } from 'pinia'

/** 全应用共用的 Pinia 实例，供路由守卫恢复当前用户状态。 */
export const pinia = createPinia()
