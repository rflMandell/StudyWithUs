import { api } from './api';
import { GlobalRole } from '../types/auth';

export interface AdminSessionInfo {
  userId: string;
  username: string;
  email: string;
  name: string;
  globalRole: GlobalRole;
  totpEnabled: boolean;
  totpEligible: boolean;
  accessTokenTtlSeconds: number;
}

export interface TwoFactorSetupResponse {
  secret: string;
  otpAuthUri: string;
}

export interface AdminUserSummary {
  id: string;
  username: string;
  name: string;
  email: string;
  status: 'ACTIVE' | 'SUSPENDED' | 'DELETED';
  globalRole: GlobalRole;
  plan?: 'FREE' | 'PREMIUM' | string;
  lifetimePoints: number;
  createdAt: string;
}

export interface AdminUser360 {
  id: string;
  username: string;
  name: string;
  email: string;
  birthDate?: string;
  avatarUrl?: string;
  institution?: string;
  course?: string;
  studyGoal?: string;
  bio?: string;
  status: 'ACTIVE' | 'SUSPENDED' | 'DELETED';
  globalRole: GlobalRole;
  totpEnabled: boolean;
  plan: string;
  subscriptionStatus: string;
  subscriptionExpiresAt?: string;
  lifetimePoints: number;
  currentStreak: number;
  longestStreak: number;
  level: number;
  totalXp: number;
  leagueDivision: string;
  communities: Array<{
    communityId: string;
    name: string;
    role: string;
    status: string;
    joinedAt: string;
  }>;
  reports: Array<{
    id: string;
    targetType: string;
    targetId: string;
    reason: string;
    status: string;
    isReporter: boolean;
    createdAt: string;
  }>;
  createdAt: string;
  updatedAt: string;
  deletedAt?: string;
}

export interface AdminUserFilters {
  status?: string;
  globalRole?: string;
  plan?: string;
  createdFrom?: string;
  createdTo?: string;
  search?: string;
}

export interface SystemLogItem {
  id: string;
  timestamp: string;
  level: 'INFO' | 'WARN' | 'ERROR' | string;
  module: string;
  traceId: string;
  requestId?: string;
  userId?: string;
  httpMethod?: string;
  path?: string;
  statusCode?: number;
  durationMs?: number;
  message: string;
  context?: string;
}

export interface SystemLogCursorPage {
  items: SystemLogItem[];
  nextCursor?: string;
  hasNext: boolean;
}

export interface SystemLogFilters {
  level?: string;
  module?: string;
  traceId?: string;
  userId?: string;
  statusCode?: number;
  cursor?: string;
  limit?: number;
}

export interface AdminAuditLogItem {
  id: string;
  actorId?: string;
  actorRole?: string;
  action: string;
  targetType: string;
  targetId?: string;
  reason?: string;
  beforeState?: string;
  afterState?: string;
  ipAddress?: string;
  userAgent?: string;
  requestId?: string;
  metadata?: string;
  createdAt: string;
}

export interface PagedAdminAuditLogs {
  content: AdminAuditLogItem[];
  page: number;
  size: number;
  totalElements: number;
  totalPages: number;
  hasNext: boolean;
}

export interface AdminAuditLogFilters {
  actorId?: string;
  action?: string;
  targetType?: string;
  targetId?: string;
  from?: string;
  to?: string;
}

export interface PagedAdminUsers {
  content: AdminUserSummary[];
  page: number;
  size: number;
  totalElements: number;
  totalPages: number;
  hasNext: boolean;
}

export const isAdminRole = (role?: GlobalRole): boolean => {
  return role === 'SUPPORT' || role === 'ADMIN' || role === 'SUPER_ADMIN';
};

export const adminService = {
  async getSession(): Promise<AdminSessionInfo> {
    const response = await api.get<AdminSessionInfo>('/admin/security/session');
    return response.data;
  },

  async setupTwoFactor(): Promise<TwoFactorSetupResponse> {
    const response = await api.post<TwoFactorSetupResponse>('/admin/security/2fa/setup');
    return response.data;
  },

  async verifyTwoFactor(code: string): Promise<{ totpEnabled: boolean }> {
    const response = await api.post<{ totpEnabled: boolean }>('/admin/security/2fa/verify', { code });
    return response.data;
  },

  async disableTwoFactor(code: string): Promise<void> {
    await api.delete('/admin/security/2fa', { data: { code } });
  },

  async listUsers(page = 0, size = 20, filters: AdminUserFilters = {}): Promise<PagedAdminUsers> {
    const params: Record<string, string | number> = { page, size };
    if (filters.status) params.status = filters.status;
    if (filters.globalRole) params.globalRole = filters.globalRole;
    if (filters.plan) params.plan = filters.plan;
    if (filters.createdFrom) params.createdFrom = filters.createdFrom;
    if (filters.createdTo) params.createdTo = filters.createdTo;
    if (filters.search?.trim()) params.search = filters.search.trim();

    const response = await api.get<PagedAdminUsers>('/admin/users', { params });
    return response.data;
  },

  async getUser360(userId: string): Promise<AdminUser360> {
    const response = await api.get<AdminUser360>(`/admin/users/${userId}`);
    return response.data;
  },

  async suspendUser(userId: string, reason: string): Promise<AdminUserSummary> {
    const response = await api.post<AdminUserSummary>(`/admin/users/${userId}/suspend`, { reason });
    return response.data;
  },

  async reactivateUser(userId: string, reason: string): Promise<AdminUserSummary> {
    const response = await api.post<AdminUserSummary>(`/admin/users/${userId}/reactivate`, { reason });
    return response.data;
  },

  async revokeSessions(userId: string, reason: string): Promise<{ userId: string; sessionsRevoked: boolean }> {
    const response = await api.post<{ userId: string; sessionsRevoked: boolean }>(
      `/admin/users/${userId}/revoke-sessions`,
      { reason }
    );
    return response.data;
  },

  async changeRole(userId: string, globalRole: GlobalRole, reason: string): Promise<AdminUserSummary> {
    const response = await api.patch<AdminUserSummary>(`/admin/users/${userId}/role`, { globalRole, reason });
    return response.data;
  },

  async listSystemLogs(filters: SystemLogFilters = {}): Promise<SystemLogCursorPage> {
    const params: Record<string, string | number> = { limit: filters.limit ?? 30 };
    if (filters.level) params.level = filters.level;
    if (filters.module?.trim()) params.module = filters.module.trim();
    if (filters.traceId?.trim()) params.traceId = filters.traceId.trim();
    if (filters.userId?.trim()) params.userId = filters.userId.trim();
    if (filters.statusCode) params.statusCode = filters.statusCode;
    if (filters.cursor) params.cursor = filters.cursor;

    const response = await api.get<SystemLogCursorPage>('/admin/system-logs', { params });
    return response.data;
  },

  async getTraceTimeline(traceId: string): Promise<SystemLogItem[]> {
    const response = await api.get<SystemLogItem[]>(`/admin/system-logs/${encodeURIComponent(traceId)}`);
    return response.data;
  },

  async listAuditLogs(page = 0, size = 20, filters: AdminAuditLogFilters = {}): Promise<PagedAdminAuditLogs> {
    const params: Record<string, string | number> = { page, size };
    if (filters.actorId?.trim()) params.actorId = filters.actorId.trim();
    if (filters.action?.trim()) params.action = filters.action.trim();
    if (filters.targetType?.trim()) params.targetType = filters.targetType.trim();
    if (filters.targetId?.trim()) params.targetId = filters.targetId.trim();
    if (filters.from) params.from = filters.from;
    if (filters.to) params.to = filters.to;

    const response = await api.get<PagedAdminAuditLogs>('/admin/audit-logs', { params });
    return response.data;
  },

  async exportAuditLogs(format: 'csv' | 'json', filters: AdminAuditLogFilters = {}): Promise<Blob> {
    const params: Record<string, string> = { format };
    if (filters.actorId?.trim()) params.actorId = filters.actorId.trim();
    if (filters.action?.trim()) params.action = filters.action.trim();
    if (filters.targetType?.trim()) params.targetType = filters.targetType.trim();
    if (filters.targetId?.trim()) params.targetId = filters.targetId.trim();

    const response = await api.get<Blob>('/admin/audit-logs/export', {
      params,
      responseType: 'blob',
    });
    return response.data;
  },
};
