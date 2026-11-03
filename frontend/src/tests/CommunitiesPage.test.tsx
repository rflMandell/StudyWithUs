import { render, screen, waitFor, fireEvent } from '@testing-library/react';
import { BrowserRouter } from 'react-router-dom';
import { describe, it, expect, vi, beforeEach } from 'vitest';
import { CommunitiesPage } from '../pages/CommunitiesPage';
import { communityService } from '../services/communityService';
import { AuthContext, AuthContextType } from '../context/AuthContext';
import { Community } from '../types/community';

vi.mock('../services/communityService', () => ({
  communityService: {
    listCommunities: vi.fn(),
    joinCommunity: vi.fn(),
  },
}));

const mockUser = {
  id: 'user-b',
  username: 'student_b',
  name: 'Student B',
  email: 'b@uni.edu',
  status: 'ACTIVE' as const,
  lifetimePoints: 50,
  createdAt: '2026-10-30T10:00:00Z',
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

const mockCommunityList: Community[] = [
  {
    id: 'comm-1',
    name: 'Grupo de Física 1',
    description: 'Resolução de exercícios semanais',
    visibility: 'PUBLIC',
    ownerId: 'user-a',
    rankingType: 'WEEKLY',
    timezone: 'UTC',
    membersCount: 1,
    currentUserRole: null,
    createdAt: '2026-10-30T10:00:00Z',
  },
  {
    id: 'comm-2',
    name: 'Algoritmos Avançados',
    description: 'Estudos de grafos e programação dinâmica',
    visibility: 'PUBLIC',
    ownerId: 'user-b',
    rankingType: 'WEEKLY',
    timezone: 'UTC',
    membersCount: 5,
    currentUserRole: 'OWNER',
    createdAt: '2026-10-30T10:00:00Z',
  },
];

const renderPage = () => {
  return render(
    <BrowserRouter>
      <AuthContext.Provider value={mockAuthContext}>
        <CommunitiesPage />
      </AuthContext.Provider>
    </BrowserRouter>
  );
};

describe('CommunitiesPage', () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  it('renders communities list and cards', async () => {
    vi.mocked(communityService.listCommunities).mockResolvedValue({
      content: mockCommunityList,
      page: 0,
      size: 20,
      totalElements: 2,
      totalPages: 1,
      hasNext: false,
    });

    renderPage();

    expect(screen.getByText(/comunidades acadêmicas/i)).toBeInTheDocument();
    expect(screen.getByRole('button', { name: /criar comunidade/i })).toBeInTheDocument();

    await waitFor(() => {
      expect(screen.getByText('Grupo de Física 1')).toBeInTheDocument();
      expect(screen.getByText('Algoritmos Avançados')).toBeInTheDocument();
    });

    expect(screen.getByRole('button', { name: /participar/i })).toBeInTheDocument();
    expect(screen.getByText('Owner')).toBeInTheDocument();
  });

  it('calls joinCommunity when user clicks Participar', async () => {
    vi.mocked(communityService.listCommunities).mockResolvedValue({
      content: mockCommunityList,
      page: 0,
      size: 20,
      totalElements: 2,
      totalPages: 1,
      hasNext: false,
    });
    vi.mocked(communityService.joinCommunity).mockResolvedValue({
      communityId: 'comm-1',
      userId: 'user-b',
      username: 'student_b',
      name: 'Student B',
      role: 'MEMBER',
      status: 'ACTIVE',
      joinedAt: '2026-10-30T12:00:00Z',
    });

    renderPage();

    await waitFor(() => {
      expect(screen.getByRole('button', { name: /participar/i })).toBeInTheDocument();
    });

    const joinBtn = screen.getByRole('button', { name: /participar/i });
    fireEvent.click(joinBtn);

    await waitFor(() => {
      expect(communityService.joinCommunity).toHaveBeenCalledWith('comm-1');
    });
  });
});
