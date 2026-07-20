import http from '@/services/http'
import type { ApiResult, Department, DepartmentCreateRequest, DepartmentUpdateRequest, PageQuery, PageResult, UserAccount, UserCreateRequest, UserUpdateRequest } from '@/types/api'

export async function fetchDepartments() {
  const response = await http.get<ApiResult<Department[]>>('/department')
  return response.data.data
}
export async function fetchDepartment(id: number) { const response = await http.get<ApiResult<Department>>(`/department/${id}`); return response.data.data }
export async function fetchDepartmentPage(params: PageQuery) { const response = await http.get<ApiResult<PageResult<Department>>>('/department/page', { params }); return response.data.data }
export async function createDepartment(payload: DepartmentCreateRequest) { const response = await http.post<ApiResult<Department>>('/department', payload); return response.data.data }
export async function updateDepartment(payload: DepartmentUpdateRequest) { const response = await http.put<ApiResult<Department>>('/department', payload); return response.data.data }
export async function disableDepartment(id: number) { await http.delete<ApiResult<null>>(`/department/${id}`) }
export async function enableDepartment(id: number) { await http.put<ApiResult<null>>(`/department/${id}/enable`) }

export async function fetchUser(id: number) { const response = await http.get<ApiResult<UserAccount>>(`/user-account/${id}`); return response.data.data }
export async function fetchUserPage(params: PageQuery) { const response = await http.get<ApiResult<PageResult<UserAccount>>>('/user-account/page', { params }); return response.data.data }
export async function createUser(payload: UserCreateRequest) { const response = await http.post<ApiResult<UserAccount>>('/user-account', payload); return response.data.data }
export async function updateUser(payload: UserUpdateRequest) { const response = await http.put<ApiResult<UserAccount>>('/user-account', payload); return response.data.data }
export async function disableUser(id: number) { await http.delete<ApiResult<null>>(`/user-account/${id}`) }
export async function enableUser(id: number) { await http.put<ApiResult<null>>(`/user-account/${id}/enable`) }
