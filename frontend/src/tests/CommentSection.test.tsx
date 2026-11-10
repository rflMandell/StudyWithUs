import { describe, it, expect, vi, beforeEach } from 'vitest';
import { render, screen, fireEvent, waitFor } from '@testing-library/react';
import { CommentSection } from '../components/CommentSection';
import { commentService } from '../services/commentService';
import { Comment } from '../types/comment';

vi.mock('../services/commentService');

describe('CommentSection', () => {
  const checkinId = 'checkin-123';
  const currentUserId = 'user-me';

  const mockComment: Comment = {
    id: 'comment-1',
    checkinId: 'checkin-123',
    author: {
      id: 'user-me',
      username: 'me_user',
      name: 'Meu Nome',
      avatarUrl: null,
    },
    content: 'Ótimo estudo de algoritmos!',
    createdAt: new Date().toISOString(),
    updatedAt: new Date().toISOString(),
  };

  beforeEach(() => {
    vi.clearAllMocks();
  });

  it('renders trigger button with comment count and toggles open to load comments', async () => {
    vi.mocked(commentService.listComments).mockResolvedValue({
      content: [mockComment],
      page: 0,
      size: 20,
      totalElements: 1,
      totalPages: 1,
      hasNext: false,
    });

    render(
      <CommentSection
        checkinId={checkinId}
        initialCommentsCount={1}
        isMember={true}
        currentUserId={currentUserId}
      />
    );

    const toggleButton = screen.getByRole('button', { name: /1 comentário/i });
    expect(toggleButton).toBeInTheDocument();

    fireEvent.click(toggleButton);

    await waitFor(() => {
      expect(commentService.listComments).toHaveBeenCalledWith(checkinId);
      expect(screen.getByText('Ótimo estudo de algoritmos!')).toBeInTheDocument();
      expect(screen.getByText('Meu Nome')).toBeInTheDocument();
    });
  });

  it('allows active member to submit a new comment and updates list', async () => {
    vi.mocked(commentService.listComments).mockResolvedValue({
      content: [],
      page: 0,
      size: 20,
      totalElements: 0,
      totalPages: 0,
      hasNext: false,
    });

    const newCommentResponse: Comment = {
      id: 'comment-2',
      checkinId,
      author: {
        id: 'user-me',
        username: 'me_user',
        name: 'Meu Nome',
        avatarUrl: null,
      },
      content: 'Parabéns pela dedicação!',
      createdAt: new Date().toISOString(),
      updatedAt: new Date().toISOString(),
    };

    vi.mocked(commentService.createComment).mockResolvedValue(newCommentResponse);

    render(
      <CommentSection
        checkinId={checkinId}
        initialCommentsCount={0}
        isMember={true}
        currentUserId={currentUserId}
      />
    );

    fireEvent.click(screen.getByRole('button', { name: /comentar/i }));

    await waitFor(() => {
      expect(screen.getByPlaceholderText(/escreva um comentário/i)).toBeInTheDocument();
    });

    const input = screen.getByPlaceholderText(/escreva um comentário/i);
    fireEvent.change(input, { target: { value: 'Parabéns pela dedicação!' } });

    const submitButton = screen.getByRole('button', { name: /enviar/i });
    fireEvent.click(submitButton);

    await waitFor(() => {
      expect(commentService.createComment).toHaveBeenCalledWith(checkinId, {
        content: 'Parabéns pela dedicação!',
      });
      expect(screen.getByText('Parabéns pela dedicação!')).toBeInTheDocument();
    });
  });

  it('allows author to delete own comment', async () => {
    vi.spyOn(window, 'confirm').mockImplementation(() => true);

    vi.mocked(commentService.listComments).mockResolvedValue({
      content: [mockComment],
      page: 0,
      size: 20,
      totalElements: 1,
      totalPages: 1,
      hasNext: false,
    });

    vi.mocked(commentService.deleteComment).mockResolvedValue(undefined);

    render(
      <CommentSection
        checkinId={checkinId}
        initialCommentsCount={1}
        isMember={true}
        currentUserId={currentUserId}
      />
    );

    fireEvent.click(screen.getByRole('button', { name: /1 comentário/i }));

    await waitFor(() => {
      expect(screen.getByText('Ótimo estudo de algoritmos!')).toBeInTheDocument();
    });

    const deleteBtn = screen.getByTitle('Excluir comentário');
    fireEvent.click(deleteBtn);

    await waitFor(() => {
      expect(commentService.deleteComment).toHaveBeenCalledWith('comment-1');
      expect(screen.queryByText('Ótimo estudo de algoritmos!')).not.toBeInTheDocument();
    });
  });
});
