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

export interface TenantRegistrationRequest {
  tenantCode: string
  tenantName: string
  adminUsername: string
  adminPassword: string
  adminDisplayName: string
  adminEmail?: string
  adminPhone?: string
}

export interface TenantRegistrationResponse {
  tenantId: number
  tenantCode: string
  adminUserId: number
  adminUsername: string
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

export interface TicketCreateRequest {
  handlingDepartmentId: number
  title: string
  description: string
  ticketType: string
  priority: TicketPreview['priority']
}

export interface TicketAssignmentRequest {
  toDepartmentId: number
  toAssigneeId?: number
  reason?: string
  version: number
}

export type TicketAction = 'ACCEPT' | 'START_PROCESS' | 'SUBMIT_RESOLUTION' | 'CONFIRM' | 'REJECT' | 'CLOSE'

export interface TicketTransitionRequest {
  action: TicketAction
  version: number
  remark?: string
  resolution?: string
}

export interface TicketCommentCreateRequest {
  content: string
  visibility: 'PUBLIC' | 'INTERNAL'
}

export interface TicketTransitionRecord {
  id: number
  action: string
  fromStatus: string
  toStatus: string
  operatorId: number
  remark: string | null
  ticketVersion: number
  createdAt: string
}

export interface TicketAssignmentRecord {
  id: number
  assignmentType: string
  fromDepartmentId: number | null
  toDepartmentId: number
  fromAssigneeId: number | null
  toAssigneeId: number | null
  assignedBy: number
  reason: string | null
  ticketVersion: number
  createdAt: string
}

export interface TicketComment {
  id: number
  authorId: number
  content: string
  visibility: 'PUBLIC' | 'INTERNAL'
  createdAt: string
}

export interface NotificationItem {
  id: number
  tenantId: number
  recipientId: number
  notificationType: string
  title: string
  content: string
  businessType: string
  businessId: number
  sourceEventId: string
  readAt: string | null
  createdAt: string
}

export interface Department {
  id: number
  tenantId: number
  parentId: number | null
  departmentCode: string
  departmentName: string
  leaderUserId: number | null
  sortOrder: number
  status: 'ACTIVE' | 'DISABLED'
  createdAt: string
  updatedAt: string
}

export interface DepartmentCreateRequest {
  parentId?: number
  departmentCode: string
  departmentName: string
  sortOrder: number
}

export interface DepartmentUpdateRequest {
  id: number
  departmentName: string
  sortOrder: number
}

export interface UserAccount {
  id: number
  tenantId: number
  departmentId: number
  username: string
  displayName: string
  email: string | null
  phone: string | null
  status: 'ACTIVE' | 'LOCKED' | 'DISABLED'
  lastLoginAt: string | null
  createdAt: string
  updatedAt: string
}

export interface UserCreateRequest {
  departmentId: number
  username: string
  rawPassword: string
  displayName: string
  email?: string
  phone?: string
}

export interface UserUpdateRequest {
  id: number
  departmentId: number
  displayName: string
  email?: string
  phone?: string
}

export interface Role {
  id: number
  tenantId: number
  roleCode: string
  roleName: string
  description: string | null
  builtIn: boolean
  status: 'ACTIVE' | 'DISABLED'
  createdAt: string
  updatedAt: string
}

export interface Permission {
  id: number
  permissionCode: string
  permissionName: string
  resource: string
  action: string
  scope: string
  description: string | null
  status: string
  createdAt: string
  updatedAt: string
}

export interface RolePermissionRelation {
  id: number
  tenantId: number
  roleId: number
  permissionId: number
  grantedBy: number | null
  grantedAt: string
}

export interface UserRoleRelation {
  id: number
  tenantId: number
  userId: number
  roleId: number
  assignedBy: number | null
  assignedAt: string
}

export interface SlaPolicy {
  id: number
  tenantId: number
  policyName: string
  priority: TicketPreview['priority']
  responseMinutes: number
  resolutionMinutes: number
  enabled: boolean
  version: number
  createdAt: string
  updatedAt: string
}

export interface Tenant {
  id: number
  tenantCode: string
  tenantName: string
  status: 'ACTIVE' | 'DISABLED'
  contactName: string | null
  contactEmail: string | null
  contactPhone: string | null
  createdAt: string
  updatedAt: string
}

export interface PageQuery {
  pageNum: number
  pageSize: number
  [key: string]: string | number | boolean | undefined
}
