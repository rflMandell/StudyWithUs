import { render, screen, waitFor, fireEvent } from '@testing-library/react';
import { MemoryRouter, Route, Routes } from 'react-router-dom';
import { describe, it, expect, vi, beforeEach } from 'vitest';
import { CommunityDetailPage } from '../pages/CommunityDetailPage';
import { communityService } from '../services/communityService';
import { checkinService } from '../services/checkinService';
import { AuthContext, AuthContextType } from '../context/AuthContext';
import { Community, CommunityJoinRequest, CommunityMember } from '../types/community';
import { Checkin } from '../types/checkin';

vi.mock('../services/communityService', () => ({
  communityService: {
    getCommunityDetails: vi.fn(),
    listMembers: vi.fn(),
    listJoinRequests: vi.fn(),
    approveJoinRequest: vi.fn(),
    rejectJoinRequest: vi.fn(),
    updateMemberRole: vi.fn(),
    removeMember: vi.fn(),
    joinCommunity: vi.fn(),
    requestJoin: vi.fn(),
    leaveCommunity: vi.fn(),
  },
}));

vi.mock('../services/checkinService', () => ({
  checkinService: {
    listCommunityCheckins: vi.fn(),
    createCheckin: vi.fn(),
    deleteCheckin: vi.fn(),
    getCheckinDetails: vi.fn(),
  },
}));

const mockUser = {
  id: 'owner-id',
  username: 'owner_user',
  name: 'Owner User',
  email: 'owner@uni.edu',
  status: 'ACTIVE' as const,
  lifetimePoints: 100,
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
  clearError: vi.fn(),
};

const mockCommunity: Community = {
  id: 'comm-10',
  name: 'Comunidade de Redes',
  description: 'Estudos de arquitetura de redes',
  visibility: 'PRIVATE',
  ownerId: 'owner-id',
  rankingType: 'WEEKLY',
  timezone: 'America/Sao_Paulo',
  membersCount: 2,
  currentUserRole: 'OWNER',
  createdAt: '2026-10-31T10:00:00Z',
};

const mockMembers: CommunityMember[] = [
  {
    communityId: 'comm-10',
    userId: 'owner-id',
    username: 'owner_user',
    name: 'Owner User',
    role: 'OWNER',
    status: 'ACTIVE',
    joinedAt: '2026-10-31T10:00:00Z',
  },
  {
    communityId: 'comm-10',
    userId: 'member-2',
    username: 'colleague_bob',
    name: 'Bob Colleague',
    role: 'MEMBER',
    status: 'ACTIVE',
    joinedAt: '2026-10-31T11:00:00Z',
  },
];

const mockJoinRequests: CommunityJoinRequest[] = [
  {
    id: 'req-1',
    communityId: 'comm-10',
    userId: 'user-candidate',
    status: 'PENDING',
    createdAt: '2026-10-31T12:00:00Z',
  },
];

const mockCheckins: Checkin[] = [
  {
    id: 'checkin-1',
    userId: 'owner-id',
    communityId: 'comm-10',
    caption: 'Estudo do modelo OSI finalizado',
    mediaKeys: ['osi_layer.png'],
    createdAt: '2026-10-31T13:00:00Z',
    updatedAt: '2026-10-31T13:00:00Z',
  },
];

const renderDetail = () => {
  return render(
    <MemoryRouter initialEntries={['/communities/comm-10']}>
      <AuthContext.Provider value={mockAuthContext}>
        <Routes>
          <Route path="/communities/:id" element={<CommunityDetailPage />} />
        </Routes>
      </AuthContext.Provider>
    </MemoryRouter>
  );
};

describe('CommunityDetailPage', () => {
  beforeEach(() => {
    vi.clearAllMocks();
    vi.mocked(checkinService.listCommunityCheckins).mockResolvedValue({
      content: mockCheckins,
      page: 0,
      size: 20,
      totalElements: 1,
      totalPages: 1,
      hasNext: false,
    });
  });

  it('renders community details, check-in feed, and owner tabs', async () => {
    vi.mocked(communityService.getCommunityDetails).mockResolvedValue(mockCommunity);
    vi.mocked(communityService.listMembers).mockResolvedValue({
      content: mockMembers,
      page: 0,
      size: 20,
      totalElements: 2,
      totalPages: 1,
      hasNext: false,
    });
    vi.mocked(communityService.listJoinRequests).mockResolvedValue({
      content: mockJoinRequests,
      page: 0,
      size: 20,
      totalElements: 1,
      totalPages: 1,
      hasNext: false,
    });

    renderDetail();

    await waitFor(() => {
      expect(screen.getByText('Comunidade de Redes')).toBeInTheDocument();
      expect(screen.getByText('Estudos de arquitetura de redes')).toBeInTheDocument();
      expect(screen.getByText('Estudo do modelo OSI finalizado')).toBeInTheDocument();
      expect(screen.getByText(/Solicitações Pendentes \(1\)/)).toBeInTheDocument();
    });
  });

  it('switches to requests tab and approves pending request', async () => {
    vi.mocked(communityService.getCommunityDetails).mockResolvedValue(mockCommunity);
    vi.mocked(communityService.listMembers).mockResolvedValue({
      content: mockMembers,
      page: 0,
      size: 20,
      totalElements: 2,
      totalPages: 1,
      hasNext: false,
    });
    vi.mocked(communityService.listJoinRequests).mockResolvedValue({
      content: mockJoinRequests,
      page: 0,
      size: 20,
      totalElements: 1,
      totalPages: 1,
      hasNext: false,
    });
    vi.mocked(communityService.approveJoinRequest).mockResolvedValue({
      ...mockJoinRequests[0],
      status: 'APPROVED',
    });

    renderDetail();

    await waitFor(() => {
      expect(screen.getByText(/Solicitações Pendentes/)).toBeInTheDocument();
    });

    fireEvent.click(screen.getByText(/Solicitações Pendentes/));

    await waitFor(() => {
      expect(screen.getByText('Aprovar')).toBeInTheDocument();
    });

    fireEvent.click(screen.getByText('Aprovar'));

    await waitFor(() => {
      expect(communityService.approveJoinRequest).toHaveBeenCalledWith('comm-10', 'req-1');
    });
  });

  it('allows member to publish a new check-in', async () => {
    vi.mocked(communityService.getCommunityDetails).mockResolvedValue(mockCommunity);
    vi.mocked(communityService.listMembers).mockResolvedValue({
      content: mockMembers,
      page: 0,
      size: 20,
      totalElements: 2,
      totalPages: 1,
      hasNext: false,
    });
    vi.mocked(communityService.listJoinRequests).mockResolvedValue({
      content: mockJoinRequests,
      page: 0,
      size: 20,
      totalElements: 1,
      totalPages: 1,
      hasNext: false,
    });
    vi.mocked(checkinService.createCheckin).mockResolvedValue({
      id: 'checkin-2',
      userId: 'owner-id',
      communityId: 'comm-10',
      caption: 'Estudo de TCP/IP',
      createdAt: '2026-10-31T14:00:00Z',
      updatedAt: '2026-10-31T14:00:00Z',
    });

    renderDetail();

    await waitFor(() => {
      expect(screen.getByPlaceholderText(/O que você estudou hoje\?/)).toBeInTheDocument();
    });

    const textarea = screen.getByPlaceholderText(/O que você estudou hoje\?/);
    fireEvent.change(textarea, { target: { value: 'Estudo de TCP/IP' } });

    const submitBtn = screen.getByText('Publicar Check-in');
    fireEvent.click(submitBtn);

    await waitFor(() => {
      expect(checkinService.createCheckin).toHaveBeenCalledWith(
        expect.objectContaining({
          communityId: 'comm-10',
          caption: 'Estudo de TCP/IP',
        }),
        expect.any(String)
      );
    });
  });
});
