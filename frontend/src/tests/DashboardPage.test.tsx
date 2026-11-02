import { render, screen, waitFor } from '@testing-library/react';
import { BrowserRouter } from 'react-router-dom';
import { describe, it, expect, vi, beforeEach } from 'vitest';
import { DashboardPage } from '../pages/DashboardPage';
import { pointsService } from '../services/pointsService';
import { AuthContext, AuthContextType } from '../context/AuthContext';
import { PointTransaction } from '../types/points';

vi.mock('../services/pointsService', () => ({
  pointsService: {
    getPointsSummary: vi.fn(),
    getPointsHistory: vi.fn(),
  },
}));

const mockUser = {
  id: 'user-points-test',
  username: 'points_tester',
  name: 'Points Tester',
  email: 'tester@uni.edu',
  status: 'ACTIVE' as const,
  lifetimePoints: 20,
  createdAt: '2026-11-01T10:00:00Z',
  institution: 'Unicamp',
  course: 'Ciência da Computação',
};

const mockAuthContext: AuthContextType = {
  user: mockUser,
  isAuthenticated: true,
  isLoading: false,
  error: null,
  login: vi.fn(),
  register: vi.fn(),
  logout: vi.fn(),
  clearError: vi.fn(),
};

const mockTransactions: PointTransaction[] = [
  {
    id: 'tx-1',
    userId: 'user-points-test',
    amount: 10,
    type: 'CHECKIN',
    createdAt: '2026-11-02T10:00:00Z',
  },
  {
    id: 'tx-2',
    userId: 'user-points-test',
    amount: -10,
    type: 'REVERSAL',
    createdAt: '2026-11-02T11:00:00Z',
  },
];

const renderPage = () => {
  return render(
    <BrowserRouter>
      <AuthContext.Provider value={mockAuthContext}>
        <DashboardPage />
      </AuthContext.Provider>
    </BrowserRouter>
  );
};

describe('DashboardPage', () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  it('renders dashboard with points summary and transactions ledger', async () => {
    vi.mocked(pointsService.getPointsSummary).mockResolvedValue({
      userId: 'user-points-test',
      currentPoints: 0,
      lifetimePoints: 20,
    });
    vi.mocked(pointsService.getPointsHistory).mockResolvedValue({
      content: mockTransactions,
      page: 0,
      size: 10,
      totalElements: 2,
      totalPages: 1,
      hasNext: false,
    });

    renderPage();

    expect(screen.getByText(/Olá, Points Tester!/)).toBeInTheDocument();
    expect(screen.getByText('Unicamp')).toBeInTheDocument();

    await waitFor(() => {
      expect(screen.getByText('Extrato de Pontos')).toBeInTheDocument();
      expect(screen.getByText('Check-in Realizado')).toBeInTheDocument();
      expect(screen.getByText('Estorno de Check-in')).toBeInTheDocument();
      expect(screen.getByText('+10 pts')).toBeInTheDocument();
      expect(screen.getByText('-10 pts')).toBeInTheDocument();
    });
  });
});
