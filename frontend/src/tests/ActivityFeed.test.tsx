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

  it('renders empty feed state when user has no activities in their communities', async () => {
    vi.mocked(feedService.getFeed).mockResolvedValue({
      content: [],
      nextCursor: null,
      hasMore: false,
    });

    renderComponent();

    await waitFor(() => {
      expect(screen.getByText('Nenhuma atividade recente encontrada nas suas comunidades.')).toBeInTheDocument();
      expect(screen.getByText('Explorar Comunidades')).toBeInTheDocument();
    });
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
});
