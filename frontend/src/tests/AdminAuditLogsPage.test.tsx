import { describe, it, expect, vi, beforeEach } from 'vitest';
import { render, screen, waitFor, fireEvent } from '@testing-library/react';
import { MemoryRouter } from 'react-router-dom';
import { AdminAuditLogsPage } from '../pages/AdminAuditLogsPage';
import { AuthContext, AuthContextType } from '../context/AuthContext';
import { adminService } from '../services/adminService';

vi.mock('../services/adminService', async () => {
  const actual = await vi.importActual<typeof import('../services/adminService')>('../services/adminService');
  return {
    ...actual,
    adminService: {
      ...actual.adminService,
      listAuditLogs: vi.fn(),
      exportAuditLogs: vi.fn(),
    },
  };
});

const authValue: AuthContextType = {
  user: {
    id: 'admin-1',
    username: 'super_admin',
    name: 'Super Admin',
    email: 'super@studywithus.com',
    status: 'ACTIVE',
    globalRole: 'SUPER_ADMIN',
    lifetimePoints: 0,
    createdAt: '2026-01-01T00:00:00Z',
  },
  isAuthenticated: true,
  isLoading: false,
  error: null,
  login: vi.fn(),
  register: vi.fn(),
  logout: vi.fn(),
  updateUser: vi.fn(),
  clearError: vi.fn(),
};

describe('AdminAuditLogsPage', () => {
  beforeEach(() => {
    vi.clearAllMocks();
    vi.mocked(adminService.listAuditLogs).mockResolvedValue({
      content: [
        {
          id: 'aud-1',
          actorId: 'admin-1',
          actorRole: 'SUPER_ADMIN',
          action: 'USER_GLOBAL_ROLE_CHANGED',
          targetType: 'USER',
          targetId: 'user-99',
          reason: 'Promoção para suporte',
          beforeState: '{"globalRole":"USER"}',
          afterState: '{"globalRole":"SUPPORT"}',
          ipAddress: '10.0.0.1',
          requestId: 'req-abc',
          createdAt: '2026-11-23T22:00:00Z',
        },
      ],
      page: 0,
      size: 20,
      totalElements: 1,
      totalPages: 1,
      hasNext: false,
    });
  });

  it('renders audit logs and opens visual diff modal', async () => {
    render(
      <AuthContext.Provider value={authValue}>
        <MemoryRouter initialEntries={['/admin/audit']}>
          <AdminAuditLogsPage />
        </MemoryRouter>
      </AuthContext.Provider>
    );

    await waitFor(() => {
      expect(screen.getByText('USER_GLOBAL_ROLE_CHANGED')).toBeInTheDocument();
      expect(screen.getByText('Promoção para suporte')).toBeInTheDocument();
    });

    fireEvent.click(screen.getByText('Ver Diff'));

    await waitFor(() => {
      expect(screen.getByText('Diff de Auditoria: USER_GLOBAL_ROLE_CHANGED')).toBeInTheDocument();
      expect(screen.getByText('"USER"')).toBeInTheDocument();
      expect(screen.getByText('"SUPPORT"')).toBeInTheDocument();
    });
  });
});
