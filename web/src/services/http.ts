import axios, { AxiosError } from 'axios'
import { ACCESS_TOKEN_KEY, LAST_TRACE_ID_KEY } from '@/constants/auth'
import type { ApiResult } from '@/types/api'

export class ApiError extends Error {
  constructor(
    public readonly code: number,
    message: string,
    public readonly httpStatus?: number,
    public readonly traceId?: string,
  ) {
    super(message)
    this.name = 'ApiError'
  }
}

const http = axios.create({
  baseURL: import.meta.env.VITE_API_BASE_URL || '/api',
  timeout: 15_000,
  headers: {
    'Content-Type': 'application/json',
  },
})

http.interceptors.request.use((config) => {
  const accessToken = sessionStorage.getItem(ACCESS_TOKEN_KEY)
  if (accessToken) {
    config.headers.Authorization = `Bearer ${accessToken}`
  }
  return config
})

http.interceptors.response.use(
  (response) => {
    const traceId = response.headers['x-trace-id'] as string | undefined
    if (traceId) {
      sessionStorage.setItem(LAST_TRACE_ID_KEY, traceId)
    }

    const result = response.data as ApiResult<unknown>
    if (typeof result?.code === 'number' && result.code !== 0) {
      if (result.code === 10002) {
        sessionStorage.removeItem(ACCESS_TOKEN_KEY)
      }
      throw new ApiError(result.code, result.msg, response.status, traceId)
    }
    return response
  },
  (error: AxiosError<ApiResult<unknown>>) => {
    const status = error.response?.status
    const result = error.response?.data
    const traceId = error.response?.headers['x-trace-id'] as string | undefined

    if (traceId) {
      sessionStorage.setItem(LAST_TRACE_ID_KEY, traceId)
    }
    if (status === 401 || result?.code === 10002) {
      sessionStorage.removeItem(ACCESS_TOKEN_KEY)
    }

    return Promise.reject(
      new ApiError(
        result?.code ?? 50000,
        result?.msg ?? '网络连接失败，请稍后重试',
        status,
        traceId,
      ),
    )
  },
)

export default http
