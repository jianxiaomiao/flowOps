import { computed, ref } from 'vue'
import { defineStore } from 'pinia'
import http from '@/services/http'
import { ACCESS_TOKEN_KEY, EXPIRES_AT_KEY, TENANT_CODE_KEY, USERNAME_KEY } from '@/constants/auth'
import type { ApiResult, CurrentUserResponse, LoginRequest, LoginResponse } from '@/types/api'

export const useAuthStore = defineStore('auth', () => {
  const accessToken = ref(sessionStorage.getItem(ACCESS_TOKEN_KEY) ?? '')
  const expiresAt = ref(sessionStorage.getItem(EXPIRES_AT_KEY) ?? '')
  const username = ref(sessionStorage.getItem(USERNAME_KEY) ?? '')
  const tenantCode = ref(sessionStorage.getItem(TENANT_CODE_KEY) ?? '')
  const userId = ref<number | null>(null)
  const tenantId = ref<number | null>(null)
  const authorities = ref<string[]>([])
  const currentUserLoaded = ref(false)
  const isAuthenticated = computed(() => Boolean(accessToken.value))

  function hasAuthority(authority: string) {
    return authorities.value.includes(authority)
  }

  async function loadCurrentUser(force = false) {
    if (!accessToken.value) return null
    if (currentUserLoaded.value && !force) {
      return {
        userId: userId.value,
        tenantId: tenantId.value,
        username: username.value,
        authorities: authorities.value,
      }
    }

    try {
      const response = await http.get<ApiResult<CurrentUserResponse>>('/auth/me')
      const currentUser = response.data.data
      userId.value = currentUser.userId
      tenantId.value = currentUser.tenantId
      username.value = currentUser.username
      authorities.value = currentUser.authorities
      currentUserLoaded.value = true
      sessionStorage.setItem(USERNAME_KEY, currentUser.username)
      return currentUser
    } catch (error) {
      logout()
      throw error
    }
  }

  async function login(payload: LoginRequest) {
    const response = await http.post<ApiResult<LoginResponse>>('/auth/login', payload)
    const loginData = response.data.data
    accessToken.value = loginData.accessToken
    expiresAt.value = loginData.expiresAt
    username.value = payload.username
    tenantCode.value = payload.tenantCode
    sessionStorage.setItem(ACCESS_TOKEN_KEY, loginData.accessToken)
    sessionStorage.setItem(EXPIRES_AT_KEY, loginData.expiresAt)
    sessionStorage.setItem(USERNAME_KEY, payload.username)
    sessionStorage.setItem(TENANT_CODE_KEY, payload.tenantCode)
    await loadCurrentUser(true)
  }

  function logout() {
    accessToken.value = ''
    expiresAt.value = ''
    username.value = ''
    tenantCode.value = ''
    userId.value = null
    tenantId.value = null
    authorities.value = []
    currentUserLoaded.value = false
    sessionStorage.removeItem(ACCESS_TOKEN_KEY)
    sessionStorage.removeItem(EXPIRES_AT_KEY)
    sessionStorage.removeItem(USERNAME_KEY)
    sessionStorage.removeItem(TENANT_CODE_KEY)
  }

  return {
    accessToken,
    expiresAt,
    username,
    tenantCode,
    userId,
    tenantId,
    authorities,
    currentUserLoaded,
    isAuthenticated,
    hasAuthority,
    loadCurrentUser,
    login,
    logout,
  }
})
