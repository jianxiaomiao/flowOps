import http from '@/services/http'
import type {
  ApiResult,
  CaptchaResponse,
  CurrentUserResponse,
  EmailCodeSendResponse,
  LoginRequest,
  LoginResponse,
  SendEmailCodeRequest,
  TenantRegistrationRequest,
  TenantRegistrationResponse,
} from '@/types/api'

export async function loginApi(payload: LoginRequest) {
  const response = await http.post<ApiResult<LoginResponse>>('/auth/login', payload)
  return response.data.data
}

export async function fetchCurrentUser() {
  const response = await http.get<ApiResult<CurrentUserResponse>>('/auth/me')
  return response.data.data
}

export async function logoutApi() {
  await http.post<ApiResult<null>>('/auth/logout')
}

export async function fetchCaptcha() {
  const response = await http.get<ApiResult<CaptchaResponse>>('/auth/captcha')
  return response.data.data
}

export async function sendRegisterEmailCode(payload: SendEmailCodeRequest) {
  const response = await http.post<ApiResult<EmailCodeSendResponse>>('/auth/email-codes', payload)
  return response.data.data
}

export async function registerTenant(payload: TenantRegistrationRequest) {
  const response = await http.post<ApiResult<TenantRegistrationResponse>>('/auth/register', payload)
  return response.data.data
}
