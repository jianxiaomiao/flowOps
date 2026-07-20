import http from '@/services/http'
import type { ApiResult, PageQuery, PageResult, Tenant } from '@/types/api'

type TenantCreate = Pick<Tenant, 'tenantCode' | 'tenantName'> & Partial<Pick<Tenant, 'contactName' | 'contactEmail' | 'contactPhone'>>
type TenantUpdate = Pick<Tenant, 'id' | 'tenantName'> & Partial<Pick<Tenant, 'contactName' | 'contactEmail' | 'contactPhone'>>

export async function fetchTenants() { const response = await http.get<ApiResult<Tenant[]>>('/tenant'); return response.data.data }
export async function fetchTenant(id: number) { const response = await http.get<ApiResult<Tenant>>(`/tenant/${id}`); return response.data.data }
export async function fetchTenantPage(params: PageQuery) { const response = await http.get<ApiResult<PageResult<Tenant>>>('/tenant/page', { params }); return response.data.data }
export async function createTenant(payload: TenantCreate) { const response = await http.post<ApiResult<Tenant>>('/tenant', payload); return response.data.data }
export async function updateTenant(payload: TenantUpdate) { const response = await http.put<ApiResult<Tenant>>('/tenant', payload); return response.data.data }
export async function disableTenant(id: number) { await http.delete<ApiResult<null>>(`/tenant/${id}`) }
export async function enableTenant(id: number) { await http.put<ApiResult<null>>(`/tenant/${id}/enable`) }
