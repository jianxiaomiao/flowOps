import http from '@/services/http'
import type { ApiResult, KnowledgeArticle, KnowledgeArticlePageQuery, KnowledgeArticlePayload, KnowledgeCategory, PageResult } from '@/types/api'

export async function fetchKnowledgeCategories() {
  const response = await http.get<ApiResult<KnowledgeCategory[]>>('/knowledge/categories')
  return response.data.data
}

export async function createKnowledgeCategory(payload: { categoryCode: string; categoryName: string; description?: string; sortOrder: number }) {
  const response = await http.post<ApiResult<KnowledgeCategory>>('/knowledge/categories', payload)
  return response.data.data
}

export async function fetchKnowledgePage(query: KnowledgeArticlePageQuery) {
  const response = await http.get<ApiResult<PageResult<KnowledgeArticle>>>('/knowledge/articles/page', { params: query })
  return response.data.data
}

export async function createKnowledgeArticle(payload: KnowledgeArticlePayload) {
  const response = await http.post<ApiResult<KnowledgeArticle>>('/knowledge/articles', payload)
  return response.data.data
}

export async function updateKnowledgeArticle(payload: KnowledgeArticlePayload & { id: number; version: number }) {
  const response = await http.put<ApiResult<KnowledgeArticle>>('/knowledge/articles', payload)
  return response.data.data
}

export async function publishKnowledgeArticle(id: number, version: number) {
  const response = await http.post<ApiResult<KnowledgeArticle>>(`/knowledge/articles/${id}/publish`, { version })
  return response.data.data
}

export async function archiveKnowledgeArticle(id: number, version: number) {
  const response = await http.post<ApiResult<KnowledgeArticle>>(`/knowledge/articles/${id}/archive`, { version })
  return response.data.data
}

export async function linkKnowledgeArticleToTicket(articleId: number, ticketId: number, relationType: 'REFERENCE' | 'SOLUTION' = 'REFERENCE') {
  await http.post<ApiResult<void>>(`/knowledge/articles/${articleId}/tickets/${ticketId}`, { relationType })
}
