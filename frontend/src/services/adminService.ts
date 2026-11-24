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
  lifetimePoints: number;
  createdAt: string;
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

  async listUsers(page = 0, size = 20): Promise<PagedAdminUsers> {
    const response = await api.get<PagedAdminUsers>('/admin/users', {
      params: { page, size },
    });
    return response.data;
  },
};
