import { render, screen, waitFor, fireEvent } from '@testing-library/react';
import { BrowserRouter } from 'react-router-dom';
import { describe, it, expect, vi, beforeEach } from 'vitest';
import { ActivityFeed } from '../components/ActivityFeed';
import { feedService } from '../services/feedService';
import { FeedItem } from '../types/feed';

vi.mock('../services/feedService', () => ({
  feedService: {
    getFeed: vi.fn(),
  },
}));

vi.mock('../hooks/useAuth', () => ({
  useAuth: () => ({
    user: { id: 'user-1', username: 'test_user', name: 'Test User' },
    isAuthenticated: true,
    isLoading: false,
    error: null,
  }),
}));

const mockFeedItems: FeedItem[] = [
  {
    id: 'item-1',
    userId: 'user-1',
    authorUsername: 'marcos_dev',
    authorName: 'Marcos Silva',
    authorAvatarUrl: null,
    communityId: 'comm-1',
    communityName: 'Algoritmos USP',
    caption: 'Finalizei 3 problemas de grafos hoje!',
    mediaKeys: ['grafo.png'],
    createdAt: '2026-11-03T14:00:00Z',
  },
];

const renderComponent = () => {
  return render(
    <BrowserRouter>
      <ActivityFeed />
    </BrowserRouter>
  );
};

describe('ActivityFeed', () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  it('renders empty feed state when user has no activities in their communities or followed users', async () => {
    vi.mocked(feedService.getFeed).mockResolvedValue({
      content: [],
      nextCursor: null,
      hasMore: false,
    });

    renderComponent();

    await waitFor(() => {
      expect(screen.getByText('Feed de Atividades')).toBeInTheDocument();
      expect(screen.getByText('Nenhuma atividade recente encontrada.')).toBeInTheDocument();
      expect(
        screen.getByText('Participe de comunidades ou siga outros usuários para acompanhar os estudos no seu feed!')
      ).toBeInTheDocument();
      expect(screen.getByText('Explorar Comunidades')).toBeInTheDocument();
    });
  });

  it('renders posts from followed users and communities with profile links', async () => {
    vi.mocked(feedService.getFeed).mockResolvedValueOnce({
      content: [
        ...mockFeedItems,
        {
          id: 'item-followed',
          userId: 'user-followed-99',
          authorUsername: 'rafa_studies',
          authorName: 'Rafael Costa',
          authorAvatarUrl: null,
          communityId: 'comm-2',
          communityName: 'Cálculo I',
          caption: 'Resumo de derivadas concluído!',
          mediaKeys: [],
          createdAt: '2026-11-03T15:00:00Z',
        },
      ],
      nextCursor: null,
      hasMore: false,
    });

    renderComponent();

    await waitFor(() => {
      expect(screen.getByText('Rafael Costa')).toBeInTheDocument();
      expect(screen.getByText('@rafa_studies')).toBeInTheDocument();
      expect(screen.getByText('Resumo de derivadas concluído!')).toBeInTheDocument();
    });

    const authorLink = screen.getByRole('link', { name: 'Rafael Costa' });
    expect(authorLink).toHaveAttribute('href', '/profile/user-followed-99');

    const usernameLink = screen.getByRole('link', { name: '@rafa_studies' });
    expect(usernameLink).toHaveAttribute('href', '/profile/user-followed-99');
  });

  it('renders feed items and handles cursor pagination correctly', async () => {
    vi.mocked(feedService.getFeed).mockResolvedValueOnce({
      content: mockFeedItems,
      nextCursor: 'cursor-token-123',
      hasMore: true,
    });

    renderComponent();

    await waitFor(() => {
      expect(screen.getByText('Marcos Silva')).toBeInTheDocument();
      expect(screen.getByText('@marcos_dev')).toBeInTheDocument();
      expect(screen.getByText('Algoritmos USP')).toBeInTheDocument();
      expect(screen.getByText('Finalizei 3 problemas de grafos hoje!')).toBeInTheDocument();
      expect(screen.getByText('1 anexo(s) incluído(s)')).toBeInTheDocument();
      expect(screen.getByText('Carregar mais atividades')).toBeInTheDocument();
    });

    // Mock page 2 response
    vi.mocked(feedService.getFeed).mockResolvedValueOnce({
      content: [
        {
          id: 'item-2',
          userId: 'user-2',
          authorUsername: 'ana_ufmg',
          authorName: 'Ana Souza',
          authorAvatarUrl: null,
          communityId: 'comm-1',
          communityName: 'Algoritmos USP',
          caption: 'Revisão de árvore binária de busca.',
          mediaKeys: [],
          createdAt: '2026-11-03T13:00:00Z',
        },
      ],
      nextCursor: null,
      hasMore: false,
    });

    fireEvent.click(screen.getByText('Carregar mais atividades'));

    await waitFor(() => {
      expect(feedService.getFeed).toHaveBeenCalledWith('cursor-token-123', 10);
      expect(screen.getByText('Ana Souza')).toBeInTheDocument();
      expect(screen.getByText('Revisão de árvore binária de busca.')).toBeInTheDocument();
      expect(screen.queryByText('Carregar mais atividades')).not.toBeInTheDocument();
    });
  });

  it('renders standalone posts with Post pessoal badge, image, and CommentSection without broken community link', async () => {
    vi.mocked(feedService.getFeed).mockResolvedValueOnce({
      content: [
        {
          id: 'standalone-post-1',
          userId: 'user-1',
          authorUsername: 'marcos_dev',
          authorName: 'Marcos Silva',
          authorAvatarUrl: null,
          communityId: null,
          communityName: null,
          caption: 'Meu resumo individual de Banco de Dados!',
          mediaKeys: ['uploads/checkin/db.webp'],
          mediaUrls: ['http://localhost:9000/uploads/checkin/db.webp'],
          commentsCount: 2,
          likesCount: 5,
          likedByCurrentUser: false,
          createdAt: '2026-11-04T12:00:00Z',
        },
      ],
      nextCursor: null,
      hasMore: false,
    });

    renderComponent();

    await waitFor(() => {
      expect(screen.getByText('Meu resumo individual de Banco de Dados!')).toBeInTheDocument();
      expect(screen.getByText('Post pessoal')).toBeInTheDocument();
    });

    expect(screen.queryByRole('link', { name: /Comunidade/i })).not.toBeInTheDocument();
    const links = screen.getAllByRole('link');
    links.forEach((link) => {
      expect(link.getAttribute('href')).not.toContain('/communities/null');
    });

    const img = screen.getByAltText('Foto do check-in 1') as HTMLImageElement;
    expect(img).toHaveAttribute('src', 'http://localhost:9000/uploads/checkin/db.webp');

    expect(screen.getByText('5 curtidas')).toBeInTheDocument();
    expect(screen.getByText(/2 comentário/i)).toBeInTheDocument();
  });

  it('does not render +10 pts badge for standalone post with countsAsCheckin: false', async () => {
    vi.mocked(feedService.getFeed).mockResolvedValueOnce({
      content: [
        {
          id: 'standalone-no-pts',
          userId: 'user-1',
          authorUsername: 'marcos_dev',
          authorName: 'Marcos Silva',
          authorAvatarUrl: null,
          communityId: null,
          communityName: null,
          caption: 'Post social sem check-in de estudo.',
          mediaKeys: [],
          countsAsCheckin: false,
          createdAt: '2026-11-04T13:00:00Z',
        },
      ],
      nextCursor: null,
      hasMore: false,
    });

    renderComponent();

    await waitFor(() => {
      expect(screen.getByText('Post social sem check-in de estudo.')).toBeInTheDocument();
      expect(screen.getByText('Post pessoal')).toBeInTheDocument();
    });

    expect(screen.queryByText('+10 pts')).not.toBeInTheDocument();
  });
});
