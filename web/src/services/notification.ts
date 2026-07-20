import http from '@/services/http'
import type { ApiResult, NotificationItem, PageResult } from '@/types/api'

export async function fetchNotificationPage(pageNum = 1, pageSize = 10, unreadOnly = false) {
  const response = await http.get<ApiResult<PageResult<NotificationItem>>>('/notification/page', {
    params: { pageNum, pageSize, unreadOnly },
  })
  return response.data.data
}

export async function markNotificationRead(id: number) {
  await http.put<ApiResult<null>>(`/notification/${id}/read`)
}
