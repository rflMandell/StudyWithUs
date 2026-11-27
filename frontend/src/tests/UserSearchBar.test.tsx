import { render, screen, waitFor, fireEvent } from '@testing-library/react';
import { MemoryRouter, Route, Routes } from 'react-router-dom';
import { describe, it, expect, vi, beforeEach } from 'vitest';
import { UserSearchBar } from '../components/UserSearchBar';
import { userService } from '../services/userService';
import { AuthContext, AuthContextType } from '../context/AuthContext';
import { User } from '../types/auth';

vi.mock('../services/userService', () => ({
  userService: {
    searchUsers: vi.fn(),
  },
}));

const mockCurrentUser: User = {
  id: 'current-user-1',
  username: 'current_user',
  name: 'Current User',
  email: 'current@usp.br',
  status: 'ACTIVE',
  lifetimePoints: 50,
  createdAt: '2026-10-01T10:00:00Z',
};

const mockAuthContext: AuthContextType = {
  user: mockCurrentUser,
  isAuthenticated: true,
  isLoading: false,
  error: null,
  login: vi.fn(),
  register: vi.fn(),
  logout: vi.fn(),
  updateUser: vi.fn(),
  clearError: vi.fn(),
};

const renderSearchBar = () => {
  return render(
    <MemoryRouter initialEntries={['/']}>
      <AuthContext.Provider value={mockAuthContext}>
        <UserSearchBar />
        <Routes>
          <Route path="/" element={<div>Home</div>} />
          <Route path="/profile/:userId" element={<div>Viewed User Profile</div>} />
          <Route path="/profile" element={<div>Own Profile</div>} />
        </Routes>
      </AuthContext.Provider>
    </MemoryRouter>
  );
};

describe('UserSearchBar', () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  it('searches by name prefix and displays matching users below the search bar', async () => {
    vi.mocked(userService.searchUsers).mockResolvedValue([
      {
        id: 'user-ra-1',
        username: 'rafael_m',
        name: 'Rafael Mendes',
        avatarUrl: null,
        isPrivate: false,
      },
      {
        id: 'user-ra-2',
        username: 'raquel_s',
        name: 'Raquel Souza',
        avatarUrl: null,
        isPrivate: true,
      },
    ]);

    renderSearchBar();

    const input = screen.getByLabelText('Buscar perfis');
    fireEvent.change(input, { target: { value: 'Ra' } });

    await waitFor(() => {
      expect(userService.searchUsers).toHaveBeenCalledWith('Ra');
      expect(screen.getByText('Rafael Mendes')).toBeInTheDocument();
      expect(screen.getByText('@rafael_m')).toBeInTheDocument();
      expect(screen.getByText('Raquel Souza')).toBeInTheDocument();
      expect(screen.getByText('@raquel_s')).toBeInTheDocument();
      expect(screen.getByLabelText('Perfil privado')).toBeInTheDocument();
    });
  });

  it('supports @ username search and navigates to /profile/:id on click', async () => {
    vi.mocked(userService.searchUsers).mockResolvedValue([
      {
        id: 'user-ra-1',
        username: 'rafael_m',
        name: 'Rafael Mendes',
        avatarUrl: null,
        isPrivate: false,
      },
    ]);

    renderSearchBar();

    const input = screen.getByLabelText('Buscar perfis');
    fireEvent.change(input, { target: { value: '@raf' } });

    await waitFor(() => {
      expect(userService.searchUsers).toHaveBeenCalledWith('@raf');
      expect(screen.getByText('Rafael Mendes')).toBeInTheDocument();
    });

    fireEvent.click(screen.getByText('Rafael Mendes'));

    await waitFor(() => {
      expect(screen.getByText('Viewed User Profile')).toBeInTheDocument();
    });
  });

  it('shows empty state when no profiles match the query', async () => {
    vi.mocked(userService.searchUsers).mockResolvedValue([]);

    renderSearchBar();

    const input = screen.getByLabelText('Buscar perfis');
    fireEvent.change(input, { target: { value: 'ZzzNaoExiste' } });

    await waitFor(() => {
      expect(userService.searchUsers).toHaveBeenCalledWith('ZzzNaoExiste');
      expect(screen.getByText('Nenhum perfil encontrado.')).toBeInTheDocument();
    });
  });
});
