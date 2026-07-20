export interface ApiResult<T> {
  code: number
  msg: string
  data: T
}

export interface LoginRequest {
  tenantCode: string
  username: string
  password: string
}

export interface LoginResponse {
  accessToken: string
  tokenType: string
  expiresAt: string
}

export interface CurrentUserResponse {
  userId: number
  tenantId: number
  username: string
  authorities: string[]
}

export interface PageResult<T> {
  records: T[]
  total: number
  size: number
  current: number
  pages: number
}

export type TicketStatus =
  | 'OPEN'
  | 'ASSIGNED'
  | 'ACCEPTED'
  | 'PROCESSING'
  | 'PENDING_CONFIRMATION'
  | 'RESOLVED'
  | 'CLOSED'

export interface TicketPreview {
  id: number
  tenantId: number
  ticketNo: string
  title: string
  description: string
  ticketType: string
  priority: 'P0' | 'P1' | 'P2'
  status: TicketStatus
  creatorId: number
  creatorDepartmentId: number
  handlingDepartmentId: number
  currentAssigneeId: number | null
  firstResponseAt: string | null
  resolutionSubmittedAt: string | null
  resolvedAt: string | null
  closedAt: string | null
  responseDueAt: string | null
  resolutionDueAt: string | null
  responseSlaStatus: string
  resolutionSlaStatus: string
  resolution: string | null
  version: number
  createdAt: string
  updatedAt: string
}

export interface TicketPageQuery {
  pageNum: number
  pageSize: number
  status?: TicketStatus
  priority?: TicketPreview['priority']
  creatorId?: number
  assigneeId?: number
  handlingDepartmentId?: number
  title?: string
}
