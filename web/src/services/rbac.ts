import http from '@/services/http'
import type { ApiResult, PageQuery, PageResult, Permission, Role, RolePermissionRelation, UserRoleRelation } from '@/types/api'

export async function fetchRole(id: number) { const response = await http.get<ApiResult<Role>>(`/role/${id}`); return response.data.data }
export async function fetchRolePage(params: PageQuery) { const response = await http.get<ApiResult<PageResult<Role>>>('/role/page', { params }); return response.data.data }
export async function createRole(payload: { roleCode: string; roleName: string; description?: string }) { const response = await http.post<ApiResult<Role>>('/role', payload); return response.data.data }
export async function updateRole(payload: { id: number; roleName: string; description?: string }) { const response = await http.put<ApiResult<Role>>('/role', payload); return response.data.data }
export async function disableRole(id: number) { await http.delete<ApiResult<null>>(`/role/${id}`) }
export async function enableRole(id: number) { await http.put<ApiResult<null>>(`/role/${id}/enable`) }
export async function fetchPermissionPage(params: PageQuery) { const response = await http.get<ApiResult<PageResult<Permission>>>('/permission/page', { params }); return response.data.data }
export async function fetchRolePermissions(roleId: number) { const response = await http.get<ApiResult<RolePermissionRelation[]>>('/role-permission', { params: { roleId } }); return response.data.data }
export async function grantRolePermission(roleId: number, permissionId: number) { const response = await http.post<ApiResult<RolePermissionRelation>>('/role-permission', { roleId, permissionId }); return response.data.data }
export async function revokeRolePermission(roleId: number, permissionId: number) { await http.delete<ApiResult<null>>('/role-permission', { params: { roleId, permissionId } }) }
export async function fetchUserRoles(userId: number) { const response = await http.get<ApiResult<UserRoleRelation[]>>('/user-role', { params: { userId } }); return response.data.data }
export async function grantUserRole(userId: number, roleId: number) { const response = await http.post<ApiResult<UserRoleRelation>>('/user-role', { userId, roleId }); return response.data.data }
export async function revokeUserRole(userId: number, roleId: number) { await http.delete<ApiResult<null>>('/user-role', { params: { userId, roleId } }) }
