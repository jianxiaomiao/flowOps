import http from '@/services/http'
import type { ApiResult, CurrentUserResponse, LoginRequest, LoginResponse, TenantRegistrationRequest, TenantRegistrationResponse } from '@/types/api'

export async function loginApi(payload: LoginRequest) {
  const response = await http.post<ApiResult<LoginResponse>>('/auth/login', payload)
  return response.data.data
}

export async function fetchCurrentUser() {
  const response = await http.get<ApiResult<CurrentUserResponse>>('/auth/me')
  return response.data.data
}

export async function registerTenant(payload: TenantRegistrationRequest) {
  const response = await http.post<ApiResult<TenantRegistrationResponse>>('/auth/register', payload)
  return response.data.data
}
