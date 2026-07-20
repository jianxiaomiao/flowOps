import http from '@/services/http'
import type { ApiResult, PageResult, TicketPageQuery, TicketPreview } from '@/types/api'

export async function fetchTicketPage(query: TicketPageQuery) {
  const response = await http.get<ApiResult<PageResult<TicketPreview>>>('/ticket/page', {
    params: query,
  })
  return response.data.data
}
