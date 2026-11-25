import http from '@/services/http'
import type {
  ApiResult, PageResult, TicketAssignmentRecord, TicketAssignmentRequest, TicketComment,
  TicketCommentCreateRequest, TicketCreateRequest, TicketPageQuery, TicketPreview,
  TicketTransitionRecord, TicketTransitionRequest,
  KnowledgeArticle, KnowledgeRecommendationDTO, KnowledgeRecommendationFeedbackRequest
} from '@/types/api'

export async function fetchTicketPage(query: TicketPageQuery) {
  const response = await http.get<ApiResult<PageResult<TicketPreview>>>('/ticket/page', {
    params: query,
  })
  return response.data.data
}

export async function createTicket(payload: TicketCreateRequest) {
  const response = await http.post<ApiResult<TicketPreview>>('/ticket', payload)
  return response.data.data
}

export async function fetchTicket(id: number) {
  const response = await http.get<ApiResult<TicketPreview>>(`/ticket/${id}`)
  return response.data.data
}

export async function assignTicket(id: number, payload: TicketAssignmentRequest) {
  const response = await http.post<ApiResult<TicketPreview>>(`/ticket/${id}/assignments`, payload)
  return response.data.data
}

export async function transitionTicket(id: number, payload: TicketTransitionRequest) {
  const response = await http.post<ApiResult<TicketPreview>>(`/ticket/${id}/transitions`, payload)
  return response.data.data
}

export async function addTicketComment(id: number, payload: TicketCommentCreateRequest) {
  const response = await http.post<ApiResult<TicketComment>>(`/ticket/${id}/comments`, payload)
  return response.data.data
}

export async function fetchTicketTransitions(id: number) {
  const response = await http.get<ApiResult<TicketTransitionRecord[]>>(`/ticket/${id}/transitions`)
  return response.data.data
}

export async function fetchTicketAssignments(id: number) {
  const response = await http.get<ApiResult<TicketAssignmentRecord[]>>(`/ticket/${id}/assignments`)
  return response.data.data
}

export async function fetchTicketComments(id: number) {
  const response = await http.get<ApiResult<TicketComment[]>>(`/ticket/${id}/comments`)
  return response.data.data
}

export async function fetchTicketKnowledgeArticles(id: number) {
  const response = await http.get<ApiResult<KnowledgeArticle[]>>(`/ticket/${id}/knowledge-articles`)
  return response.data.data
}

export async function fetchTicketRecommendations(id: number) {
  const response = await http.get<ApiResult<KnowledgeRecommendationDTO[]>>(`/ticket/${id}/recommendations`)
  return response.data.data
}

export async function submitRecommendationFeedback(id: number, articleId: number, payload: KnowledgeRecommendationFeedbackRequest) {
  const response = await http.post<ApiResult<void>>(`/ticket/${id}/recommendations/${articleId}/feedback`, payload)
  return response.data
}

export async function fetchExternalQueue(pageNum: number, pageSize: number, mine: boolean) {
  const response = await http.get<ApiResult<PageResult<TicketPreview>>>('/ticket/external-queue', {
    params: { pageNum, pageSize, mine },
  })
  return response.data.data
}

export async function claimExternalTicket(id: number, version: number) {
  const response = await http.post<ApiResult<TicketPreview>>(`/ticket/${id}/claim`, { version })
  return response.data.data
}
