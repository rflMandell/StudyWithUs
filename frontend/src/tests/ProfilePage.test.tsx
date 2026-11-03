import { render, screen, waitFor, fireEvent } from '@testing-library/react';
import { BrowserRouter } from 'react-router-dom';
import { describe, it, expect, vi, beforeEach } from 'vitest';
import { ProfilePage } from '../pages/ProfilePage';
import { userService } from '../services/userService';
import { AuthContext, AuthContextType } from '../context/AuthContext';
import { User } from '../types/auth';

vi.mock('../services/userService', () => ({
  userService: {
    getProfile: vi.fn(),
    updateProfile: vi.fn(),
    getUserStreak: vi.fn(),
    getUserBadges: vi.fn(),
  },
}));

const mockUser: User = {
  id: 'user-profile-test',
  username: 'mariana_silva',
  name: 'Mariana Silva',
  email: 'mariana@usp.br',
  status: 'ACTIVE',
  bio: 'Estudante de Engenharia Mecatrônica',
  institution: 'USP',
  course: 'Engenharia',
  studyGoal: 'Estágio no exterior',
  lifetimePoints: 120,
  currentPoints: 120,
  activeCommunitiesCount: 3,
  checkinsCount: 12,
  currentStreak: 5,
  longestStreak: 14,
  createdAt: '2026-10-01T10:00:00Z',
  badges: [
    {
      badgeId: 'badge-1',
      name: 'Primeiro Estudo',
      description: 'Completou o primeiro check-in de estudo',
      awardedAt: '2026-10-02T10:00:00Z',
    },
  ],
};

const mockAuthContext: AuthContextType = {
  user: mockUser,
  isAuthenticated: true,
  isLoading: false,
  error: null,
  login: vi.fn(),
  register: vi.fn(),
  logout: vi.fn(),
  updateUser: vi.fn(),
  clearError: vi.fn(),
};

const renderComponent = () => {
  return render(
    <BrowserRouter>
      <AuthContext.Provider value={mockAuthContext}>
        <ProfilePage />
      </AuthContext.Provider>
    </BrowserRouter>
  );
};

describe('ProfilePage', () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  it('renders user profile with stats and badges', async () => {
    vi.mocked(userService.getProfile).mockResolvedValue(mockUser);

    renderComponent();

    // print screen debug
    await waitFor(() => {
      expect(screen.queryByText('Carregando dados do perfil...')).not.toBeInTheDocument();
    });

    expect(screen.getAllByText('Mariana Silva').length).toBeGreaterThanOrEqual(1);
    expect(screen.getAllByText('@mariana_silva').length).toBeGreaterThanOrEqual(1);
    expect(screen.getByText('Estudante de Engenharia Mecatrônica')).toBeInTheDocument();
    expect(screen.getByText('Total vitalício: 120 pts')).toBeInTheDocument();
    expect(screen.getByText('3')).toBeInTheDocument();
    expect(screen.getByText('12')).toBeInTheDocument();
    expect(screen.getByText('Maior sequência: 14 dias')).toBeInTheDocument();
    expect(screen.getByText('Primeiro Estudo')).toBeInTheDocument();
  });

  it('switches to edit mode, updates profile and saves changes', async () => {
    vi.mocked(userService.getProfile).mockResolvedValue(mockUser);
    const updatedUser: User = {
      ...mockUser,
      name: 'Mariana Silva Santos',
      bio: 'Engenheira e pesquisadora em robótica',
    };
    vi.mocked(userService.updateProfile).mockResolvedValue(updatedUser);

    renderComponent();

    await waitFor(() => {
      expect(screen.getByText('Editar Perfil')).toBeInTheDocument();
    });

    fireEvent.click(screen.getByText('Editar Perfil'));

    expect(screen.getByText('Editar Informações Pessoais')).toBeInTheDocument();

    const nameInput = screen.getByDisplayValue('Mariana Silva');
    fireEvent.change(nameInput, { target: { value: 'Mariana Silva Santos' } });

    const bioInput = screen.getByDisplayValue('Estudante de Engenharia Mecatrônica');
    fireEvent.change(bioInput, { target: { value: 'Engenheira e pesquisadora em robótica' } });

    fireEvent.click(screen.getByText('Salvar Alterações'));

    await waitFor(() => {
      expect(userService.updateProfile).toHaveBeenCalledWith(
        expect.objectContaining({
          name: 'Mariana Silva Santos',
          bio: 'Engenheira e pesquisadora em robótica',
        })
      );
      expect(mockAuthContext.updateUser).toHaveBeenCalledWith(updatedUser);
      expect(screen.getByText('Perfil atualizado com sucesso!')).toBeInTheDocument();
    });
  });
});
