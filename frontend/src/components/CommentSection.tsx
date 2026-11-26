import React, { useMemo, useState } from 'react';
import { MessageSquare, Send, Trash2, ChevronDown, ChevronUp, Heart, CornerDownRight, X } from 'lucide-react';
import { Comment } from '../types/comment';
import { commentService } from '../services/commentService';
import { checkinService } from '../services/checkinService';

interface CommentSectionProps {
  checkinId: string;
  initialCommentsCount?: number;
  initialLikesCount?: number;
  initialLikedByCurrentUser?: boolean;
  isMember?: boolean;
  currentUserId?: string;
}

export const CommentSection: React.FC<CommentSectionProps> = ({
  checkinId,
  initialCommentsCount = 0,
  initialLikesCount = 0,
  initialLikedByCurrentUser = false,
  isMember = true,
  currentUserId,
}) => {
  const [isOpen, setIsOpen] = useState(false);
  const [comments, setComments] = useState<Comment[]>([]);
  const [commentsCount, setCommentsCount] = useState(initialCommentsCount);
  const [checkinLikesCount, setCheckinLikesCount] = useState(initialLikesCount);
  const [checkinLiked, setCheckinLiked] = useState(initialLikedByCurrentUser);
  const [isTogglingCheckinLike, setIsTogglingCheckinLike] = useState(false);

  const [newComment, setNewComment] = useState('');
  const [replyingToId, setReplyingToId] = useState<string | null>(null);
  const [replyContent, setReplyContent] = useState('');
  const [isSubmittingReply, setIsSubmittingReply] = useState(false);

  const [isLoading, setIsLoading] = useState(false);
  const [isSubmitting, setIsSubmitting] = useState(false);
  const [error, setError] = useState<string | null>(null);

  const repliesByParent = useMemo(() => {
    const map = new Map<string, Comment[]>();
    for (const c of comments) {
      if (c.parentCommentId) {
        const list = map.get(c.parentCommentId) ?? [];
        list.push(c);
        map.set(c.parentCommentId, list);
      }
    }
    return map;
  }, [comments]);

  const topLevelComments = useMemo(
    () => comments.filter((c) => !c.parentCommentId),
    [comments]
  );

  const toggleComments = async () => {
    const nextState = !isOpen;
    setIsOpen(nextState);

    if (nextState && comments.length === 0) {
      setIsLoading(true);
      setError(null);
      try {
        const response = await commentService.listComments(checkinId, 0, 50);
        setComments(response.content);
        setCommentsCount(response.totalElements);
      } catch (err: any) {
        setError('Não foi possível carregar os comentários.');
      } finally {
        setIsLoading(false);
      }
    }
  };

  const handleToggleCheckinLike = async () => {
    if (isTogglingCheckinLike) return;
    setIsTogglingCheckinLike(true);
    const prevLiked = checkinLiked;
    const prevCount = checkinLikesCount;

    setCheckinLiked(!prevLiked);
    setCheckinLikesCount(prevLiked ? Math.max(0, prevCount - 1) : prevCount + 1);

    try {
      const res = await checkinService.toggleLike(checkinId);
      setCheckinLiked(res.liked);
      setCheckinLikesCount(res.likesCount);
    } catch {
      setCheckinLiked(prevLiked);
      setCheckinLikesCount(prevCount);
    } finally {
      setIsTogglingCheckinLike(false);
    }
  };

  const handleToggleCommentLike = async (commentId: string) => {
    const target = comments.find((c) => c.id === commentId);
    if (!target) return;

    const prevLiked = Boolean(target.likedByCurrentUser);
    const prevCount = target.likesCount ?? 0;

    setComments((prev) =>
      prev.map((c) =>
        c.id === commentId
          ? {
              ...c,
              likedByCurrentUser: !prevLiked,
              likesCount: prevLiked ? Math.max(0, prevCount - 1) : prevCount + 1,
            }
          : c
      )
    );

    try {
      const res = await commentService.toggleLike(commentId);
      setComments((prev) =>
        prev.map((c) =>
          c.id === commentId
            ? { ...c, likedByCurrentUser: res.liked, likesCount: res.likesCount }
            : c
        )
      );
    } catch {
      setComments((prev) =>
        prev.map((c) =>
          c.id === commentId
            ? { ...c, likedByCurrentUser: prevLiked, likesCount: prevCount }
            : c
        )
      );
    }
  };

  const handleCreateComment = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!newComment.trim()) return;

    setIsSubmitting(true);
    setError(null);
    try {
      const created = await commentService.createComment(checkinId, {
        content: newComment.trim(),
      });
      setComments((prev) => [...prev, created]);
      setCommentsCount((prev) => prev + 1);
      setNewComment('');
    } catch (err: any) {
      const msg = err.response?.data?.message || err.response?.data?.code || 'Erro ao publicar comentário.';
      setError(msg);
    } finally {
      setIsSubmitting(false);
    }
  };

  const handleCreateReply = async (e: React.FormEvent, parentCommentId: string) => {
    e.preventDefault();
    if (!replyContent.trim()) return;

    setIsSubmittingReply(true);
    setError(null);
    try {
      const created = await commentService.createComment(checkinId, {
        content: replyContent.trim(),
        parentCommentId,
      });
      setComments((prev) => [...prev, created]);
      setCommentsCount((prev) => prev + 1);
      setReplyContent('');
      setReplyingToId(null);
    } catch (err: any) {
      const msg = err.response?.data?.message || err.response?.data?.code || 'Erro ao responder comentário.';
      setError(msg);
    } finally {
      setIsSubmittingReply(false);
    }
  };

  const collectDescendantIds = (rootId: string, list: Comment[]): Set<string> => {
    const toRemove = new Set<string>([rootId]);
    let added = true;
    while (added) {
      added = false;
      for (const c of list) {
        if (c.parentCommentId && toRemove.has(c.parentCommentId) && !toRemove.has(c.id)) {
          toRemove.add(c.id);
          added = true;
        }
      }
    }
    return toRemove;
  };

  const handleDeleteComment = async (commentId: string) => {
    if (!confirm('Deseja excluir este comentário?')) return;

    try {
      await commentService.deleteComment(commentId);
      const removedIds = collectDescendantIds(commentId, comments);
      setComments((prev) => prev.filter((c) => !removedIds.has(c.id)));
      setCommentsCount((prev) => Math.max(0, prev - removedIds.size));
    } catch (err: any) {
      alert(err.response?.data?.message || 'Erro ao excluir comentário.');
    }
  };

  const renderComment = (comment: Comment, depth = 0): React.ReactNode => {
    const canDelete = currentUserId && comment.author.id === currentUserId;
    const isLiked = Boolean(comment.likedByCurrentUser);
    const likesCount = comment.likesCount ?? 0;
    const replies = repliesByParent.get(comment.id) ?? [];
    const isReplyingHere = replyingToId === comment.id;

    return (
      <div
        key={comment.id}
        style={{
          marginLeft: depth > 0 ? '1.25rem' : 0,
          borderLeft: depth > 0 ? '2px solid #cbd5e1' : 'none',
          paddingLeft: depth > 0 ? '0.65rem' : 0,
          display: 'flex',
          flexDirection: 'column',
          gap: '0.4rem',
        }}
      >
        <div
          style={{
            backgroundColor: depth > 0 ? '#f1f5f9' : '#f8fafc',
            borderRadius: '6px',
            padding: '0.6rem 0.75rem',
            display: 'flex',
            flexDirection: 'column',
            gap: '0.25rem',
          }}
        >
          <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
            <div style={{ display: 'flex', alignItems: 'center', gap: '0.4rem' }}>
              <div
                style={{
                  width: '24px',
                  height: '24px',
                  borderRadius: '50%',
                  backgroundColor: '#e2e8f0',
                  color: '#475569',
                  display: 'flex',
                  alignItems: 'center',
                  justifyContent: 'center',
                  fontSize: '0.75rem',
                  fontWeight: 600,
                  overflow: 'hidden',
                  flexShrink: 0,
                }}
              >
                {comment.author.avatarUrl ? (
                  <img
                    src={comment.author.avatarUrl}
                    alt={comment.author.name || 'Avatar'}
                    style={{ width: '100%', height: '100%', objectFit: 'cover' }}
                  />
                ) : (
                  (comment.author.name || 'U').charAt(0).toUpperCase()
                )}
              </div>
              <span style={{ fontWeight: 600, fontSize: '0.85rem', color: '#1e293b' }}>
                {comment.author.name}
              </span>
              <span style={{ fontSize: '0.75rem', color: 'var(--text-muted)' }}>
                @{comment.author.username}
              </span>
            </div>

            <div style={{ display: 'flex', alignItems: 'center', gap: '0.5rem' }}>
              <span style={{ fontSize: '0.72rem', color: 'var(--text-muted)' }}>
                {new Date(comment.createdAt).toLocaleDateString()}
              </span>
              {canDelete && (
                <button
                  type="button"
                  onClick={() => handleDeleteComment(comment.id)}
                  title="Excluir comentário"
                  style={{
                    background: 'none',
                    border: 'none',
                    color: '#dc2626',
                    cursor: 'pointer',
                    padding: '2px',
                    display: 'flex',
                    alignItems: 'center',
                  }}
                >
                  <Trash2 size={13} />
                </button>
              )}
            </div>
          </div>

          <p style={{ fontSize: '0.85rem', color: '#334155', margin: 0, whiteSpace: 'pre-wrap' }}>
            {comment.content}
          </p>

          <div style={{ display: 'flex', alignItems: 'center', gap: '0.85rem', marginTop: '0.2rem' }}>
            <button
              type="button"
              onClick={() => handleToggleCommentLike(comment.id)}
              aria-label={isLiked ? 'Descurtir comentário' : 'Curtir comentário'}
              style={{
                background: 'none',
                border: 'none',
                cursor: 'pointer',
                display: 'inline-flex',
                alignItems: 'center',
                gap: '0.25rem',
                color: isLiked ? '#e11d48' : 'var(--text-muted)',
                fontSize: '0.75rem',
                fontWeight: isLiked ? 600 : 500,
                padding: 0,
              }}
            >
              <Heart
                size={15}
                fill={isLiked ? '#ef4444' : 'transparent'}
                stroke={isLiked ? '#ef4444' : '#64748b'}
                strokeWidth={2}
              />
              <span>{likesCount > 0 ? likesCount : 'Curtir'}</span>
            </button>

            {isMember && (
              <button
                type="button"
                onClick={() => {
                  if (isReplyingHere) {
                    setReplyingToId(null);
                    setReplyContent('');
                  } else {
                    setReplyingToId(comment.id);
                    setReplyContent('');
                  }
                }}
                style={{
                  background: 'none',
                  border: 'none',
                  cursor: 'pointer',
                  display: 'inline-flex',
                  alignItems: 'center',
                  gap: '0.25rem',
                  color: isReplyingHere ? 'var(--primary)' : 'var(--text-muted)',
                  fontSize: '0.75rem',
                  fontWeight: 500,
                  padding: 0,
                }}
              >
                <CornerDownRight size={13} />
                <span>Responder</span>
              </button>
            )}
          </div>
        </div>

        {isReplyingHere && (
          <form
            onSubmit={(e) => handleCreateReply(e, comment.id)}
            style={{
              display: 'flex',
              gap: '0.4rem',
              marginLeft: '1rem',
              alignItems: 'center',
            }}
          >
            <input
              type="text"
              className="form-control"
              placeholder={`Responder a ${comment.author.name}...`}
              value={replyContent}
              onChange={(e) => setReplyContent(e.target.value)}
              disabled={isSubmittingReply}
              style={{ fontSize: '0.8rem', padding: '0.35rem 0.55rem' }}
            />
            <button
              type="submit"
              disabled={isSubmittingReply || !replyContent.trim()}
              className="btn btn-primary"
              style={{ width: 'auto', padding: '0.35rem 0.65rem', fontSize: '0.8rem', gap: '0.25rem' }}
            >
              <Send size={13} />
              <span>Responder</span>
            </button>
            <button
              type="button"
              onClick={() => {
                setReplyingToId(null);
                setReplyContent('');
              }}
              aria-label="Cancelar resposta"
              style={{
                background: 'none',
                border: 'none',
                color: 'var(--text-muted)',
                cursor: 'pointer',
                padding: '0.25rem',
                display: 'flex',
                alignItems: 'center',
              }}
            >
              <X size={14} />
            </button>
          </form>
        )}

        {replies.length > 0 && (
          <div style={{ display: 'flex', flexDirection: 'column', gap: '0.4rem' }}>
            {replies.map((reply) => renderComment(reply, depth + 1))}
          </div>
        )}
      </div>
    );
  };

  return (
    <div style={{ marginTop: '0.75rem', borderTop: '1px solid #f1f5f9', paddingTop: '0.5rem' }}>
      <div style={{ display: 'flex', alignItems: 'center', gap: '1.1rem' }}>
        <button
          type="button"
          onClick={handleToggleCheckinLike}
          aria-label={checkinLiked ? 'Descurtir post' : 'Curtir post'}
          style={{
            background: 'none',
            border: 'none',
            cursor: 'pointer',
            display: 'flex',
            alignItems: 'center',
            gap: '0.35rem',
            color: checkinLiked ? '#e11d48' : 'var(--text-muted)',
            fontSize: '0.85rem',
            fontWeight: checkinLiked ? 600 : 500,
            padding: '0.25rem 0',
          }}
        >
          <Heart
            size={18}
            fill={checkinLiked ? '#ef4444' : 'transparent'}
            stroke={checkinLiked ? '#ef4444' : '#64748b'}
            strokeWidth={2}
          />
          <span>
            {checkinLikesCount === 0
              ? 'Curtir'
              : `${checkinLikesCount} curtida${checkinLikesCount > 1 ? 's' : ''}`}
          </span>
        </button>

        <button
          type="button"
          onClick={toggleComments}
          style={{
            background: 'none',
            border: 'none',
            cursor: 'pointer',
            display: 'flex',
            alignItems: 'center',
            gap: '0.4rem',
            color: 'var(--text-muted)',
            fontSize: '0.85rem',
            fontWeight: 500,
            padding: '0.25rem 0',
          }}
        >
          <MessageSquare size={15} />
          <span>
            {commentsCount === 0
              ? 'Comentar'
              : `${commentsCount} comentário${commentsCount > 1 ? 's' : ''}`}
          </span>
          {isOpen ? <ChevronUp size={14} /> : <ChevronDown size={14} />}
        </button>
      </div>

      {isOpen && (
        <div style={{ marginTop: '0.75rem' }}>
          {error && (
            <div
              style={{
                backgroundColor: '#fef2f2',
                color: '#dc2626',
                padding: '0.4rem 0.6rem',
                borderRadius: '4px',
                fontSize: '0.8rem',
                marginBottom: '0.5rem',
              }}
            >
              {error}
            </div>
          )}

          {isLoading ? (
            <p style={{ fontSize: '0.8rem', color: 'var(--text-muted)', margin: '0.5rem 0' }}>
              Carregando comentários...
            </p>
          ) : topLevelComments.length === 0 ? (
            <p style={{ fontSize: '0.8rem', color: 'var(--text-muted)', margin: '0.5rem 0' }}>
              Nenhum comentário ainda. Seja o primeiro a interagir!
            </p>
          ) : (
            <div style={{ display: 'flex', flexDirection: 'column', gap: '0.6rem', marginBottom: '0.75rem' }}>
              {topLevelComments.map((comment) => renderComment(comment, 0))}
            </div>
          )}

          {isMember ? (
            <form onSubmit={handleCreateComment} style={{ display: 'flex', gap: '0.5rem', marginTop: '0.5rem' }}>
              <input
                type="text"
                className="form-control"
                placeholder="Escreva um comentário de incentivo..."
                value={newComment}
                onChange={(e) => setNewComment(e.target.value)}
                disabled={isSubmitting}
                style={{ fontSize: '0.85rem', padding: '0.4rem 0.6rem' }}
              />
              <button
                type="submit"
                disabled={isSubmitting || !newComment.trim()}
                className="btn btn-primary"
                style={{ width: 'auto', padding: '0.4rem 0.75rem', fontSize: '0.85rem', gap: '0.3rem' }}
              >
                <Send size={14} />
                <span>Enviar</span>
              </button>
            </form>
          ) : (
            <p style={{ fontSize: '0.8rem', color: 'var(--text-muted)', fontStyle: 'italic', margin: '0.5rem 0' }}>
              Apenas membros da comunidade podem comentar neste check-in.
            </p>
          )}
        </div>
      )}
    </div>
  );
};
