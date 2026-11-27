import { render, screen, waitFor, fireEvent } from '@testing-library/react';
import { MemoryRouter, Route, Routes } from 'react-router-dom';
import { describe, it, expect, vi, beforeEach } from 'vitest';
import { ProfilePage } from '../pages/ProfilePage';
import { userService } from '../services/userService';
import { followService } from '../services/followService';
import { AuthContext, AuthContextType } from '../context/AuthContext';
import { User } from '../types/auth';

vi.mock('../services/userService', () => ({
  userService: {
    getProfile: vi.fn(),
    updateProfile: vi.fn(),
    uploadAvatar: vi.fn(),
    getUserStreak: vi.fn(),
    getUserBadges: vi.fn(),
    getUserCheckins: vi.fn(),
  },
}));

vi.mock('../services/followService', () => ({
  followService: {
    followUser: vi.fn(),
    unfollowUser: vi.fn(),
    getFollowStatus: vi.fn(),
    getPendingRequests: vi.fn(),
    acceptRequest: vi.fn(),
    rejectRequest: vi.fn(),
  },
}));

vi.mock('../services/mediaService', () => ({
  mediaService: {
    validateImageFile: vi.fn(),
    uploadImage: vi.fn(),
  },
}));

const authUser: User = {
  id: 'auth-user-1',
  username: 'mariana_silva',
  name: 'Mariana Silva',
  email: 'mariana@usp.br',
  status: 'ACTIVE',
  lifetimePoints: 120,
  createdAt: '2026-10-01T10:00:00Z',
};

const mockAuthContext: AuthContextType = {
  user: authUser,
  isAuthenticated: true,
  isLoading: false,
  error: null,
  login: vi.fn(),
  register: vi.fn(),
  logout: vi.fn(),
  updateUser: vi.fn(),
  clearError: vi.fn(),
};

const renderAtRoute = (initialPath: string) => {
  return render(
    <MemoryRouter initialEntries={[initialPath]}>
      <AuthContext.Provider value={mockAuthContext}>
        <Routes>
          <Route path="/profile" element={<ProfilePage />} />
          <Route path="/profile/:userId" element={<ProfilePage />} />
        </Routes>
      </AuthContext.Provider>
    </MemoryRouter>
  );
};

describe('ProfilePage Follow & Privacy', () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  it('shows full info and transitions Seguir -> Seguindo on a public profile', async () => {
    const publicUser: User = {
      id: 'user-public-2',
      username: 'lucas_dev',
      name: 'Lucas Andrade',
      email: 'lucas@usp.br',
      bio: 'Focado em algoritmos',
      institution: 'USP',
      course: 'BCC',
      studyGoal: 'ICPC',
      status: 'ACTIVE',
      isPrivate: false,
      lifetimePoints: 300,
      currentPoints: 300,
      activeCommunitiesCount: 4,
      checkinsCount: 15,
      currentStreak: 7,
      longestStreak: 10,
      createdAt: '2026-10-01T10:00:00Z',
      badges: [],
    };

    vi.mocked(userService.getProfile).mockResolvedValue(publicUser);
    vi.mocked(followService.getFollowStatus).mockResolvedValue({
      status: 'NONE',
      followersCount: 10,
      followingCount: 5,
    });
    vi.mocked(userService.getUserCheckins).mockResolvedValue([
      {
        id: 'checkin-1',
        userId: 'user-public-2',
        communityId: 'comm-1',
        communityName: 'Algoritmos USP',
        caption: 'Estudei programação dinâmica hoje',
        mediaKeys: [],
        createdAt: '2026-11-10T10:00:00Z',
      },
    ]);
    vi.mocked(followService.followUser).mockResolvedValue({
      status: 'FOLLOWING',
      followersCount: 11,
      followingCount: 5,
    });

    renderAtRoute('/profile/user-public-2');

    await waitFor(() => {
      expect(screen.getByText('Lucas Andrade')).toBeInTheDocument();
      expect(screen.getByText('Publicações de Estudo')).toBeInTheDocument();
      expect(screen.getByText('Estudei programação dinâmica hoje')).toBeInTheDocument();
      expect(screen.getByText('Seguir')).toBeInTheDocument();
    });

    fireEvent.click(screen.getByRole('button', { name: 'Seguir' }));

    await waitFor(() => {
      expect(followService.followUser).toHaveBeenCalledWith('user-public-2');
      expect(screen.getByRole('button', { name: 'Seguindo' })).toBeInTheDocument();
    });
  });

  it('hides posts/stats/badges on a private profile when not following and transitions Solicitar para seguir -> Solicitado', async () => {
    const privateUser: User = {
      id: 'user-private-3',
      username: 'julia_priv',
      name: 'Júlia Rocha',
      email: 'julia@unicamp.br',
      bio: 'Estudando para residência',
      status: 'ACTIVE',
      isPrivate: true,
      lifetimePoints: 200,
      createdAt: '2026-10-01T10:00:00Z',
    };

    vi.mocked(userService.getProfile).mockResolvedValue(privateUser);
    vi.mocked(followService.getFollowStatus).mockResolvedValue({
      status: 'NONE',
      followersCount: 3,
      followingCount: 2,
    });
    vi.mocked(followService.followUser).mockResolvedValue({
      status: 'REQUESTED',
      followersCount: 3,
      followingCount: 2,
    });

    renderAtRoute('/profile/user-private-3');

    await waitFor(() => {
      expect(screen.getByText('Júlia Rocha')).toBeInTheDocument();
      expect(screen.getByText('@julia_priv')).toBeInTheDocument();
      expect(screen.getByText('Estudando para residência')).toBeInTheDocument();
      expect(
        screen.getByText('Conta Privada — Apenas informações básicas estão visíveis')
      ).toBeInTheDocument();
    });

    expect(screen.queryByText('Publicações de Estudo')).not.toBeInTheDocument();
    expect(screen.queryByText('Pontuação')).not.toBeInTheDocument();
    expect(screen.queryByText('Conquistas & Badges')).not.toBeInTheDocument();
    expect(userService.getUserCheckins).not.toHaveBeenCalled();

    fireEvent.click(screen.getByRole('button', { name: 'Solicitar para seguir' }));

    await waitFor(() => {
      expect(followService.followUser).toHaveBeenCalledWith('user-private-3');
      expect(screen.getByRole('button', { name: 'Solicitado' })).toBeInTheDocument();
    });
  });

  it('shows pending follow requests on own profile and handles Aceitar and Recusar actions', async () => {
    vi.mocked(userService.getProfile).mockResolvedValue({
      ...authUser,
      badges: [],
    });
    vi.mocked(userService.getUserCheckins).mockResolvedValue([]);
    vi.mocked(followService.getPendingRequests).mockResolvedValue([
      {
        id: 'req-1',
        requesterId: 'u-10',
        requesterUsername: 'pedro_s',
        requesterName: 'Pedro Santos',
        requesterAvatarUrl: null,
        createdAt: '2026-11-10T10:00:00Z',
      },
      {
        id: 'req-2',
        requesterId: 'u-11',
        requesterUsername: 'carla_m',
        requesterName: 'Carla Lima',
        requesterAvatarUrl: null,
        createdAt: '2026-11-10T11:00:00Z',
      },
    ]);
    vi.mocked(followService.acceptRequest).mockResolvedValue(undefined);
    vi.mocked(followService.rejectRequest).mockResolvedValue(undefined);

    renderAtRoute('/profile');

    await waitFor(() => {
      expect(screen.getByText('Solicitações para seguir')).toBeInTheDocument();
      expect(screen.getByText('Pedro Santos')).toBeInTheDocument();
      expect(screen.getByText('Carla Lima')).toBeInTheDocument();
    });

    const acceptButtons = screen.getAllByRole('button', { name: 'Aceitar' });
    fireEvent.click(acceptButtons[0]);

    await waitFor(() => {
      expect(followService.acceptRequest).toHaveBeenCalledWith('req-1');
      expect(screen.queryByText('Pedro Santos')).not.toBeInTheDocument();
      expect(screen.getByText('Carla Lima')).toBeInTheDocument();
    });

    fireEvent.click(screen.getByRole('button', { name: 'Recusar' }));

    await waitFor(() => {
      expect(followService.rejectRequest).toHaveBeenCalledWith('req-2');
      expect(screen.queryByText('Carla Lima')).not.toBeInTheDocument();
    });
  });
});
