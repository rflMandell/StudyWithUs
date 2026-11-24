import { describe, it, expect, vi, beforeEach } from 'vitest';
import { render, screen, waitFor, fireEvent } from '@testing-library/react';
import { MemoryRouter, Routes, Route } from 'react-router-dom';
import { AuthContext, AuthContextType } from '../context/AuthContext';
import { AdminProtectedRoute } from '../components/AdminProtectedRoute';
import { AdminDashboardPage } from '../pages/AdminDashboardPage';
import { AdminUsersPage } from '../pages/AdminUsersPage';
import { AdminSystemInfoPage } from '../pages/AdminSystemInfoPage';
import { AdminSystemLogsPage } from '../pages/AdminSystemLogsPage';
import { AdminAuditLogsPage } from '../pages/AdminAuditLogsPage';
import { adminService } from '../services/adminService';

vi.mock('../services/adminService', async () => {
  const actual = await vi.importActual<typeof import('../services/adminService')>('../services/adminService');
  return {
    ...actual,
    adminService: {
      ...actual.adminService,
      getSession: vi.fn(),
      listUsers: vi.fn(),
      getSystemInfo: vi.fn(),
      getSystemHealth: vi.fn(),
      getSystemOverview: vi.fn(),
      listSystemLogs: vi.fn(),
      listAuditLogs: vi.fn(),
    },
  };
});

const adminAuth: AuthContextType = {
  user: {
    id: 'super-1',
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

describe('Admin Backoffice Full E2E Navigation Flow', () => {
  beforeEach(() => {
    vi.clearAllMocks();
    vi.mocked(adminService.getSession).mockResolvedValue({
      userId: 'super-1',
      username: 'super_admin',
      email: 'super@studywithus.com',
      name: 'Super Admin',
      globalRole: 'SUPER_ADMIN',
      totpEnabled: true,
      totpEligible: true,
      accessTokenTtlSeconds: 300,
    });
    vi.mocked(adminService.listUsers).mockResolvedValue({
      content: [
        {
          id: 'u-1',
          username: 'student1',
          name: 'Student One',
          email: 'student1@studywithus.com',
          status: 'ACTIVE',
          globalRole: 'USER',
          plan: 'FREE',
          lifetimePoints: 50,
          createdAt: '2026-01-01T00:00:00Z',
        },
      ],
      page: 0,
      size: 15,
      totalElements: 1,
      totalPages: 1,
      hasNext: false,
    });
    vi.mocked(adminService.getSystemInfo).mockResolvedValue({
      applicationName: 'studywithus-backend',
      version: '0.0.1-SNAPSHOT',
      commitSha: 'e2e-sha',
      activeProfiles: ['local'],
      uptimeSeconds: 300,
      javaVersion: '21',
      jvmName: 'OpenJDK',
      flyway: { enabled: true, currentVersion: '10', appliedMigrations: 10, pendingMigrations: 0 },
    });
    vi.mocked(adminService.getSystemHealth).mockResolvedValue({
      overallStatus: 'UP',
      checkedAt: '2026-11-23T22:20:00Z',
      database: { status: 'UP', latencyMs: 2, activeConnections: 1, idleConnections: 9, totalConnections: 10, maxPoolSize: 10 },
      redis: { status: 'UP', latencyMs: 1, usedMemory: '1.2M', cachedKeys: 10 },
      objectStorage: { status: 'UP', provider: 'S3', bucketOrPath: 'studywithus-media', latencyMs: 5 },
      outboxAndJobs: { status: 'UP', pendingOutboxEvents: 0, failedOutboxEvents: 0, recentJobs: [] },
    });
    vi.mocked(adminService.getSystemOverview).mockResolvedValue({
      totalUsers: 1,
      activeUsers: 1,
      suspendedUsers: 0,
      newUsersLast24h: 1,
      activeCommunities: 2,
      checkinsLast24h: 3,
      activePremiumSubscriptions: 0,
      pendingReports: 0,
    });
    vi.mocked(adminService.listSystemLogs).mockResolvedValue({
      items: [
        {
          id: 'log-e2e-1',
          timestamp: '2026-11-23T22:20:00Z',
          level: 'INFO',
          module: 'admin',
          traceId: 'trace-e2e-01',
          message: 'GET /api/v1/admin/users -> 200',
        },
      ],
      hasNext: false,
    });
    vi.mocked(adminService.listAuditLogs).mockResolvedValue({
      content: [
        {
          id: 'aud-e2e-1',
          actorId: 'super-1',
          actorRole: 'SUPER_ADMIN',
          action: 'USER_SUSPENDED',
          targetType: 'USER',
          targetId: 'u-1',
          reason: 'E2E audit check',
          createdAt: '2026-11-23T22:20:00Z',
        },
      ],
      page: 0,
      size: 20,
      totalElements: 1,
      totalPages: 1,
      hasNext: false,
    });
  });

  it('navigates through dashboard -> contas -> info sistema -> logs -> auditoria', async () => {
    render(
      <AuthContext.Provider value={adminAuth}>
        <MemoryRouter initialEntries={['/admin']}>
          <Routes>
            <Route path="/admin" element={<AdminProtectedRoute><AdminDashboardPage /></AdminProtectedRoute>} />
            <Route path="/admin/users" element={<AdminProtectedRoute><AdminUsersPage /></AdminProtectedRoute>} />
            <Route path="/admin/system" element={<AdminProtectedRoute><AdminSystemInfoPage /></AdminProtectedRoute>} />
            <Route path="/admin/logs" element={<AdminProtectedRoute><AdminSystemLogsPage /></AdminProtectedRoute>} />
            <Route path="/admin/audit" element={<AdminProtectedRoute><AdminAuditLogsPage /></AdminProtectedRoute>} />
          </Routes>
        </MemoryRouter>
      </AuthContext.Provider>
    );

    // 1. Dashboard (/admin)
    await waitFor(() => {
      expect(screen.getByText('Sessão Administrativa Atual')).toBeInTheDocument();
    });

    // 2. Contas (/admin/users)
    fireEvent.click(screen.getByText('Contas da Plataforma'));
    await waitFor(() => {
      expect(screen.getByText('student1@studywithus.com')).toBeInTheDocument();
    });

    // 3. Info do Sistema (/admin/system)
    fireEvent.click(screen.getByText('Info do Sistema'));
    await waitFor(() => {
      expect(screen.getByText('Geral: UP')).toBeInTheDocument();
    });

    // 4. Logs do Sistema (/admin/logs)
    fireEvent.click(screen.getByText('Logs do Sistema'));
    await waitFor(() => {
      expect(screen.getByText('GET /api/v1/admin/users -> 200')).toBeInTheDocument();
    });

    // 5. Auditoria (/admin/audit)
    fireEvent.click(screen.getByText('Auditoria'));
    await waitFor(() => {
      expect(screen.getByText('USER_SUSPENDED')).toBeInTheDocument();
    });
  });
});
