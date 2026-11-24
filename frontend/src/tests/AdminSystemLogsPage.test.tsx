import { describe, it, expect, vi, beforeEach } from 'vitest';
import { render, screen, waitFor, fireEvent } from '@testing-library/react';
import { MemoryRouter } from 'react-router-dom';
import { AdminSystemLogsPage } from '../pages/AdminSystemLogsPage';
import { AuthContext, AuthContextType } from '../context/AuthContext';
import { adminService } from '../services/adminService';

vi.mock('../services/adminService', async () => {
  const actual = await vi.importActual<typeof import('../services/adminService')>('../services/adminService');
  return {
    ...actual,
    adminService: {
      ...actual.adminService,
      listSystemLogs: vi.fn(),
      getTraceTimeline: vi.fn(),
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

describe('AdminSystemLogsPage', () => {
  beforeEach(() => {
    vi.clearAllMocks();
    vi.mocked(adminService.listSystemLogs).mockResolvedValue({
      items: [
        {
          id: 'log-1',
          timestamp: '2026-11-23T21:00:00Z',
          level: 'ERROR',
          module: 'checkin',
          traceId: 'trace-err-001',
          requestId: 'req-1',
          httpMethod: 'POST',
          path: '/api/v1/checkins',
          statusCode: 500,
          durationMs: 42,
          message: 'INTERNAL_SERVER_ERROR: simulated failure',
          context: '{"stackTrace":"Service.java:42"}',
        },
      ],
      hasNext: false,
    });

    vi.mocked(adminService.getTraceTimeline).mockResolvedValue([
      {
        id: 'log-0',
        timestamp: '2026-11-23T20:59:59Z',
        level: 'INFO',
        module: 'checkin',
        traceId: 'trace-err-001',
        message: 'Request started',
      },
      {
        id: 'log-1',
        timestamp: '2026-11-23T21:00:00Z',
        level: 'ERROR',
        module: 'checkin',
        traceId: 'trace-err-001',
        message: 'INTERNAL_SERVER_ERROR: simulated failure',
      },
    ]);
  });

  it('renders logs, expands context, and opens traceId timeline', async () => {
    render(
      <AuthContext.Provider value={authValue}>
        <MemoryRouter initialEntries={['/admin/logs']}>
          <AdminSystemLogsPage />
        </MemoryRouter>
      </AuthContext.Provider>
    );

    await waitFor(() => {
      expect(screen.getByText('INTERNAL_SERVER_ERROR: simulated failure')).toBeInTheDocument();
    });

    fireEvent.click(screen.getByLabelText('Expandir contexto'));
    expect(screen.getByText(/Service.java:42/)).toBeInTheDocument();

    fireEvent.click(screen.getByText('trace-err-001'));
    await waitFor(() => {
      expect(screen.getByText(/Timeline do Trace:/)).toBeInTheDocument();
      expect(screen.getByText('Request started')).toBeInTheDocument();
    });
  });
});
