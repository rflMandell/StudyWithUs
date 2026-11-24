import { describe, it, expect, vi, beforeEach } from 'vitest';
import { render, screen, waitFor } from '@testing-library/react';
import { MemoryRouter } from 'react-router-dom';
import { AdminSystemInfoPage } from '../pages/AdminSystemInfoPage';
import { AuthContext, AuthContextType } from '../context/AuthContext';
import { adminService } from '../services/adminService';

vi.mock('../services/adminService', async () => {
  const actual = await vi.importActual<typeof import('../services/adminService')>('../services/adminService');
  return {
    ...actual,
    adminService: {
      ...actual.adminService,
      getSystemInfo: vi.fn(),
      getSystemHealth: vi.fn(),
      getSystemOverview: vi.fn(),
    },
  };
});

const authValue: AuthContextType = {
  user: {
    id: 'admin-1',
    username: 'admin_user',
    name: 'Admin User',
    email: 'admin@studywithus.com',
    status: 'ACTIVE',
    globalRole: 'ADMIN',
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

describe('AdminSystemInfoPage', () => {
  beforeEach(() => {
    vi.clearAllMocks();
    vi.mocked(adminService.getSystemInfo).mockResolvedValue({
      applicationName: 'studywithus-backend',
      version: '0.0.1-SNAPSHOT',
      commitSha: 'abc1234',
      activeProfiles: ['local'],
      uptimeSeconds: 120,
      javaVersion: '21.0.2',
      jvmName: 'OpenJDK',
      flyway: { enabled: true, currentVersion: '10', appliedMigrations: 10, pendingMigrations: 0 },
    });
    vi.mocked(adminService.getSystemHealth).mockResolvedValue({
      overallStatus: 'DEGRADED',
      checkedAt: '2026-11-23T22:15:00Z',
      database: { status: 'UP', latencyMs: 3, activeConnections: 1, idleConnections: 9, totalConnections: 10, maxPoolSize: 10 },
      redis: { status: 'DOWN', latencyMs: 2000, error: 'Connection refused' },
      objectStorage: { status: 'UP', provider: 'S3', bucketOrPath: 'studywithus-media', latencyMs: 12 },
      outboxAndJobs: { status: 'UP', pendingOutboxEvents: 0, failedOutboxEvents: 0, recentJobs: [] },
    });
    vi.mocked(adminService.getSystemOverview).mockResolvedValue({
      totalUsers: 150,
      activeUsers: 140,
      suspendedUsers: 10,
      newUsersLast24h: 5,
      activeCommunities: 12,
      checkinsLast24h: 88,
      activePremiumSubscriptions: 22,
      pendingReports: 3,
    });
  });

  it('renders infrastructure health including DEGRADED/DOWN state without crashing', async () => {
    render(
      <AuthContext.Provider value={authValue}>
        <MemoryRouter initialEntries={['/admin/system']}>
          <AdminSystemInfoPage />
        </MemoryRouter>
      </AuthContext.Provider>
    );

    await waitFor(() => {
      expect(screen.getByText('Geral: DEGRADED')).toBeInTheDocument();
      expect(screen.getByText('Connection refused')).toBeInTheDocument();
      expect(screen.getByText('150')).toBeInTheDocument();
      expect(screen.getByText('Prometheus')).toBeInTheDocument();
      expect(screen.getByText('Grafana (Phase 3)')).toBeInTheDocument();
    });
  });
});
