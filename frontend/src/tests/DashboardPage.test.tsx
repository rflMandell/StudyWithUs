import { render, screen, waitFor, fireEvent } from '@testing-library/react';
import { BrowserRouter } from 'react-router-dom';
import { describe, it, expect, vi, beforeEach } from 'vitest';
import { DashboardPage } from '../pages/DashboardPage';
import { pointsService } from '../services/pointsService';
import { feedService } from '../services/feedService';
import { checkinService } from '../services/checkinService';
import { mediaService } from '../services/mediaService';
import { AuthContext, AuthContextType } from '../context/AuthContext';
import { PointTransaction } from '../types/points';

vi.mock('../services/pointsService', () => ({
  pointsService: {
    getPointsSummary: vi.fn(),
    getPointsHistory: vi.fn(),
  },
}));

vi.mock('../services/feedService', () => ({
  feedService: {
    getFeed: vi.fn(),
  },
}));

vi.mock('../services/checkinService', () => ({
  checkinService: {
    createCheckin: vi.fn(),
    toggleLike: vi.fn(),
  },
}));

vi.mock('../services/mediaService', () => ({
  mediaService: {
    validateImageFile: vi.fn(),
    uploadImage: vi.fn(),
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
  updateUser: vi.fn(),
  refreshUser: vi.fn(),
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
    vi.mocked(feedService.getFeed).mockResolvedValue({
      content: [
        {
          id: 'checkin-feed-1',
          userId: 'author-1',
          authorUsername: 'alice',
          authorName: 'Alice Silva',
          authorAvatarUrl: null,
          communityId: 'comm-1',
          communityName: 'Comunidade Java',
          caption: 'Estudo concluído sobre JPA e PostgreSQL!',
          mediaKeys: [],
          createdAt: '2026-11-03T10:00:00Z',
        },
      ],
      nextCursor: null,
      hasMore: false,
    });

    renderPage();

    expect(screen.getByText(/Olá, Points Tester!/)).toBeInTheDocument();
    expect(screen.getByText('Unicamp')).toBeInTheDocument();

    await waitFor(() => {
      expect(screen.getByText('Feed de Atividades')).toBeInTheDocument();
      expect(screen.getByText('Estudo concluído sobre JPA e PostgreSQL!')).toBeInTheDocument();
      expect(screen.getByText('Comunidade Java')).toBeInTheDocument();
      expect(screen.getByText('Extrato de Pontos')).toBeInTheDocument();
      expect(screen.getByText('Check-in Realizado')).toBeInTheDocument();
      expect(screen.getByText('Estorno de Check-in')).toBeInTheDocument();
      expect(screen.getAllByText('+10 pts').length).toBeGreaterThanOrEqual(1);
      expect(screen.getByText('-10 pts')).toBeInTheDocument();
    });
  });

  it('opens Novo Post modal, uploads image, creates standalone post without communityId, and refreshes feed', async () => {
    URL.createObjectURL = vi.fn(() => 'blob:preview-url');
    URL.revokeObjectURL = vi.fn();

    vi.mocked(pointsService.getPointsSummary).mockResolvedValue({
      userId: 'user-points-test',
      currentPoints: 20,
      lifetimePoints: 20,
    });
    vi.mocked(pointsService.getPointsHistory).mockResolvedValue({
      content: [],
      page: 0,
      size: 10,
      totalElements: 0,
      totalPages: 0,
      hasNext: false,
    });
    vi.mocked(feedService.getFeed)
      .mockResolvedValueOnce({
        content: [],
        nextCursor: null,
        hasMore: false,
      })
      .mockResolvedValueOnce({
        content: [
          {
            id: 'standalone-1',
            userId: 'user-points-test',
            authorUsername: 'points_tester',
            authorName: 'Points Tester',
            authorAvatarUrl: null,
            communityId: null,
            communityName: null,
            caption: 'Estudando arquitetura de software!',
            mediaKeys: ['uploads/checkin/post.webp'],
            mediaUrls: ['http://localhost:9000/uploads/checkin/post.webp'],
            createdAt: '2026-11-04T10:00:00Z',
          },
        ],
        nextCursor: null,
        hasMore: false,
      });

    vi.mocked(mediaService.uploadImage).mockResolvedValue({
      id: 'media-1',
      ownerId: 'user-points-test',
      purpose: 'CHECKIN',
      status: 'CONFIRMED',
      storageKey: 'uploads/checkin/post.webp',
      url: 'http://localhost:9000/uploads/checkin/post.webp',
    });
    vi.mocked(checkinService.createCheckin).mockResolvedValue({
      id: 'standalone-1',
      userId: 'user-points-test',
      communityId: null,
      caption: 'Estudando arquitetura de software!',
      mediaKeys: ['uploads/checkin/post.webp'],
      createdAt: '2026-11-04T10:00:00Z',
      updatedAt: '2026-11-04T10:00:00Z',
    });

    renderPage();

    const novoPostBtn = await screen.findByRole('button', { name: /Novo Post/i });
    fireEvent.click(novoPostBtn);

    expect(screen.getByRole('dialog', { name: 'Novo Post' })).toBeInTheDocument();

    const checkinCheckbox = screen.getByLabelText(/Contar como check-in de estudo/i) as HTMLInputElement;
    expect(checkinCheckbox).toBeChecked();

    const textarea = screen.getByPlaceholderText(/O que você estudou hoje/i);
    fireEvent.change(textarea, { target: { value: 'Estudando arquitetura de software!' } });

    const fileInput = screen.getByLabelText('Anexar foto do post') as HTMLInputElement;
    const file = new File(['img-bytes'], 'post.png', { type: 'image/png' });
    fireEvent.change(fileInput, { target: { files: [file] } });

    expect(mediaService.validateImageFile).toHaveBeenCalledWith(file);
    expect(screen.getByAltText('Pré-visualização do post')).toBeInTheDocument();

    fireEvent.click(screen.getByRole('button', { name: /Publicar/i }));

    await waitFor(() => {
      expect(mediaService.uploadImage).toHaveBeenCalledWith(file, 'CHECKIN');
      expect(checkinService.createCheckin).toHaveBeenCalledWith(
        {
          caption: 'Estudando arquitetura de software!',
          mediaKeys: ['uploads/checkin/post.webp'],
          countsAsCheckin: true,
        },
        expect.any(String)
      );
      expect(screen.queryByRole('dialog', { name: 'Novo Post' })).not.toBeInTheDocument();
      expect(feedService.getFeed).toHaveBeenCalledTimes(2);
      expect(screen.getByText('Estudando arquitetura de software!')).toBeInTheDocument();
      expect(screen.getByText('Post pessoal')).toBeInTheDocument();
    });
  });

  it('allows unchecking countsAsCheckin in Novo Post modal so post is created with countsAsCheckin: false', async () => {
    vi.mocked(pointsService.getPointsSummary).mockResolvedValue({
      userId: 'user-points-test',
      currentPoints: 20,
      lifetimePoints: 20,
    });
    vi.mocked(pointsService.getPointsHistory).mockResolvedValue({
      content: [],
      page: 0,
      size: 10,
      totalElements: 0,
      totalPages: 0,
      hasNext: false,
    });
    vi.mocked(feedService.getFeed).mockResolvedValue({
      content: [],
      nextCursor: null,
      hasMore: false,
    });
    vi.mocked(checkinService.createCheckin).mockResolvedValue({
      id: 'standalone-no-checkin',
      userId: 'user-points-test',
      communityId: null,
      caption: 'Apenas compartilhando uma reflexão.',
      mediaKeys: [],
      countsAsCheckin: false,
      createdAt: '2026-11-04T11:00:00Z',
      updatedAt: '2026-11-04T11:00:00Z',
    });

    renderPage();

    const novoPostBtn = await screen.findByRole('button', { name: /Novo Post/i });
    fireEvent.click(novoPostBtn);

    const checkinCheckbox = screen.getByLabelText(/Contar como check-in de estudo/i) as HTMLInputElement;
    expect(checkinCheckbox).toBeChecked();

    fireEvent.click(checkinCheckbox);
    expect(checkinCheckbox).not.toBeChecked();

    const textarea = screen.getByPlaceholderText(/O que você estudou hoje/i);
    fireEvent.change(textarea, { target: { value: 'Apenas compartilhando uma reflexão.' } });

    fireEvent.click(screen.getByRole('button', { name: /Publicar/i }));

    await waitFor(() => {
      expect(checkinService.createCheckin).toHaveBeenCalledWith(
        {
          caption: 'Apenas compartilhando uma reflexão.',
          mediaKeys: [],
          countsAsCheckin: false,
        },
        expect.any(String)
      );
      expect(screen.queryByRole('dialog', { name: 'Novo Post' })).not.toBeInTheDocument();
    });
  });
});
