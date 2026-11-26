import { describe, it, expect, vi, beforeEach } from 'vitest';
import { render, screen, fireEvent, waitFor } from '@testing-library/react';
import { CommentSection } from '../components/CommentSection';
import { commentService } from '../services/commentService';
import { checkinService } from '../services/checkinService';
import { Comment } from '../types/comment';

vi.mock('../services/commentService');
vi.mock('../services/checkinService');

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
      expect(commentService.listComments).toHaveBeenCalledWith(checkinId, 0, 50);
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

  it('renders author avatar image when avatarUrl is provided', async () => {
    const commentWithAvatar: Comment = {
      ...mockComment,
      author: {
        ...mockComment.author,
        avatarUrl: 'https://example.com/avatar.png',
      },
    };

    vi.mocked(commentService.listComments).mockResolvedValue({
      content: [commentWithAvatar],
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

    fireEvent.click(screen.getByRole('button', { name: /1 comentário/i }));

    await waitFor(() => {
      const avatarImg = screen.getByAltText('Meu Nome') as HTMLImageElement;
      expect(avatarImg).toBeInTheDocument();
      expect(avatarImg.src).toBe('https://example.com/avatar.png');
    });
  });

  it('allows toggling like on post and on a comment', async () => {
    vi.mocked(checkinService.toggleLike).mockResolvedValueOnce({
      targetId: checkinId,
      liked: true,
      likesCount: 1,
    });
    vi.mocked(checkinService.toggleLike).mockResolvedValueOnce({
      targetId: checkinId,
      liked: false,
      likesCount: 0,
    });

    vi.mocked(commentService.listComments).mockResolvedValue({
      content: [{ ...mockComment, likesCount: 0, likedByCurrentUser: false }],
      page: 0,
      size: 20,
      totalElements: 1,
      totalPages: 1,
      hasNext: false,
    });
    vi.mocked(commentService.toggleLike).mockResolvedValue({
      targetId: mockComment.id,
      liked: true,
      likesCount: 1,
    });

    render(
      <CommentSection
        checkinId={checkinId}
        initialCommentsCount={1}
        initialLikesCount={0}
        initialLikedByCurrentUser={false}
        isMember={true}
        currentUserId={currentUserId}
      />
    );

    const postLikeBtn = screen.getByRole('button', { name: /curtir post/i });
    fireEvent.click(postLikeBtn);

    await waitFor(() => {
      expect(checkinService.toggleLike).toHaveBeenCalledWith(checkinId);
      expect(screen.getByText('1 curtida')).toBeInTheDocument();
    });

    const postUnlikeBtn = screen.getByRole('button', { name: /descurtir post/i });
    fireEvent.click(postUnlikeBtn);

    await waitFor(() => {
      expect(checkinService.toggleLike).toHaveBeenCalledTimes(2);
      expect(screen.getByText('Curtir')).toBeInTheDocument();
    });

    fireEvent.click(screen.getByRole('button', { name: /1 comentário/i }));
    await waitFor(() => {
      expect(screen.getByText('Ótimo estudo de algoritmos!')).toBeInTheDocument();
    });

    const commentLikeBtn = screen.getByRole('button', { name: /curtir comentário/i });
    fireEvent.click(commentLikeBtn);

    await waitFor(() => {
      expect(commentService.toggleLike).toHaveBeenCalledWith(mockComment.id);
      expect(screen.getByRole('button', { name: /descurtir comentário/i })).toHaveTextContent('1');
    });
  });

  it('allows replying to a comment', async () => {
    vi.mocked(commentService.listComments).mockResolvedValue({
      content: [mockComment],
      page: 0,
      size: 20,
      totalElements: 1,
      totalPages: 1,
      hasNext: false,
    });

    const replyResponse: Comment = {
      id: 'reply-1',
      checkinId,
      parentCommentId: mockComment.id,
      author: {
        id: 'user-other',
        username: 'maria',
        name: 'Maria',
        avatarUrl: null,
      },
      content: 'Concordo totalmente!',
      likesCount: 0,
      likedByCurrentUser: false,
      createdAt: new Date().toISOString(),
      updatedAt: new Date().toISOString(),
    };
    vi.mocked(commentService.createComment).mockResolvedValue(replyResponse);

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

    fireEvent.click(screen.getByRole('button', { name: /responder/i }));
    const replyInput = screen.getByPlaceholderText(/responder a meu nome/i);
    fireEvent.change(replyInput, { target: { value: 'Concordo totalmente!' } });

    const submitReplyBtns = screen.getAllByRole('button', { name: /responder/i });
    fireEvent.click(submitReplyBtns[submitReplyBtns.length - 1]);

    await waitFor(() => {
      expect(commentService.createComment).toHaveBeenCalledWith(checkinId, {
        content: 'Concordo totalmente!',
        parentCommentId: mockComment.id,
      });
      expect(screen.getByText('Concordo totalmente!')).toBeInTheDocument();
    });
  });
});
