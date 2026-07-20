import http from '@/services/http'
import type { ApiResult, PageQuery, PageResult, SlaPolicy, TicketPreview } from '@/types/api'

export async function fetchSlaPolicy(id: number) { const response = await http.get<ApiResult<SlaPolicy>>(`/sla-policy/${id}`); return response.data.data }
export async function fetchSlaPage(params: PageQuery) { const response = await http.get<ApiResult<PageResult<SlaPolicy>>>('/sla-policy/page', { params }); return response.data.data }
export async function createSlaPolicy(payload: { policyName: string; priority: TicketPreview['priority']; responseMinutes: number; resolutionMinutes: number }) { const response = await http.post<ApiResult<SlaPolicy>>('/sla-policy', payload); return response.data.data }
export async function updateSlaPolicy(payload: { id: number; policyName?: string; responseMinutes?: number; resolutionMinutes?: number; enabled?: boolean; version: number }) { const response = await http.put<ApiResult<SlaPolicy>>('/sla-policy', payload); return response.data.data }
