import { describe, it, expect, vi, beforeEach } from 'vitest';
import { render, screen, waitFor, fireEvent } from '@testing-library/react';
import { MemoryRouter } from 'react-router-dom';
import { AdminUsersPage } from '../pages/AdminUsersPage';
import { AuthContext, AuthContextType } from '../context/AuthContext';
import { adminService } from '../services/adminService';

vi.mock('../services/adminService', async () => {
  const actual = await vi.importActual<typeof import('../services/adminService')>('../services/adminService');
  return {
    ...actual,
    adminService: {
      ...actual.adminService,
      listUsers: vi.fn(),
      getUser360: vi.fn(),
      suspendUser: vi.fn(),
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

describe('AdminUsersPage', () => {
  beforeEach(() => {
    vi.clearAllMocks();
    vi.mocked(adminService.listUsers).mockResolvedValue({
      content: [
        {
          id: 'u-1',
          username: 'maria',
          name: 'Maria Silva',
          email: 'maria@studywithus.com',
          status: 'ACTIVE',
          globalRole: 'USER',
          plan: 'PREMIUM',
          lifetimePoints: 120,
          createdAt: '2026-01-01T00:00:00Z',
        },
      ],
      page: 0,
      size: 15,
      totalElements: 1,
      totalPages: 1,
      hasNext: false,
    });

    vi.mocked(adminService.getUser360).mockResolvedValue({
      id: 'u-1',
      username: 'maria',
      name: 'Maria Silva',
      email: 'maria@studywithus.com',
      status: 'ACTIVE',
      globalRole: 'USER',
      totpEnabled: false,
      plan: 'PREMIUM',
      subscriptionStatus: 'ACTIVE',
      lifetimePoints: 120,
      currentStreak: 5,
      longestStreak: 10,
      level: 3,
      totalXp: 450,
      leagueDivision: 'SILVER',
      communities: [],
      reports: [],
      createdAt: '2026-01-01T00:00:00Z',
      updatedAt: '2026-01-01T00:00:00Z',
    });

    vi.mocked(adminService.suspendUser).mockResolvedValue({
      id: 'u-1',
      username: 'maria',
      name: 'Maria Silva',
      email: 'maria@studywithus.com',
      status: 'SUSPENDED',
      globalRole: 'USER',
      plan: 'PREMIUM',
      lifetimePoints: 120,
      createdAt: '2026-01-01T00:00:00Z',
    });
  });

  it('renders users table, opens 360 modal, and suspends user with mandatory reason', async () => {
    render(
      <AuthContext.Provider value={authValue}>
        <MemoryRouter initialEntries={['/admin/users']}>
          <AdminUsersPage />
        </MemoryRouter>
      </AuthContext.Provider>
    );

    await waitFor(() => {
      expect(screen.getByText('maria@studywithus.com')).toBeInTheDocument();
    });

    fireEvent.click(screen.getByText('Ver 360°'));

    await waitFor(() => {
      expect(screen.getByText('Visão 360° da Conta')).toBeInTheDocument();
      expect(screen.getByText('SILVER')).toBeInTheDocument();
    });

    fireEvent.change(screen.getByLabelText('Justificativa obrigatória para ações administrativas'), {
      target: { value: 'Comportamento abusivo' },
    });
    fireEvent.click(screen.getByText('Suspender Conta'));

    await waitFor(() => {
      expect(adminService.suspendUser).toHaveBeenCalledWith('u-1', 'Comportamento abusivo');
    });
  });
});
