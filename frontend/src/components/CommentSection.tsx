import React, { useState } from 'react';
import { MessageSquare, Send, Trash2, ChevronDown, ChevronUp } from 'lucide-react';
import { Comment } from '../types/comment';
import { commentService } from '../services/commentService';

interface CommentSectionProps {
  checkinId: string;
  initialCommentsCount?: number;
  isMember?: boolean;
  currentUserId?: string;
}

export const CommentSection: React.FC<CommentSectionProps> = ({
  checkinId,
  initialCommentsCount = 0,
  isMember = true,
  currentUserId,
}) => {
  const [isOpen, setIsOpen] = useState(false);
  const [comments, setComments] = useState<Comment[]>([]);
  const [commentsCount, setCommentsCount] = useState(initialCommentsCount);
  const [newComment, setNewComment] = useState('');
  const [isLoading, setIsLoading] = useState(false);
  const [isSubmitting, setIsSubmitting] = useState(false);
  const [error, setError] = useState<string | null>(null);

  const toggleComments = async () => {
    const nextState = !isOpen;
    setIsOpen(nextState);

    if (nextState && comments.length === 0) {
      setIsLoading(true);
      setError(null);
      try {
        const response = await commentService.listComments(checkinId);
        setComments(response.content);
        setCommentsCount(response.totalElements);
      } catch (err: any) {
        setError('Não foi possível carregar os comentários.');
      } finally {
        setIsLoading(false);
      }
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

  const handleDeleteComment = async (commentId: string) => {
    if (!confirm('Deseja excluir este comentário?')) return;

    try {
      await commentService.deleteComment(commentId);
      setComments((prev) => prev.filter((c) => c.id !== commentId));
      setCommentsCount((prev) => Math.max(0, prev - 1));
    } catch (err: any) {
      alert(err.response?.data?.message || 'Erro ao excluir comentário.');
    }
  };

  return (
    <div style={{ marginTop: '0.75rem', borderTop: '1px solid #f1f5f9', paddingTop: '0.5rem' }}>
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
          ) : comments.length === 0 ? (
            <p style={{ fontSize: '0.8rem', color: 'var(--text-muted)', margin: '0.5rem 0' }}>
              Nenhum comentário ainda. Seja o primeiro a interagir!
            </p>
          ) : (
            <div style={{ display: 'flex', flexDirection: 'column', gap: '0.6rem', marginBottom: '0.75rem' }}>
              {comments.map((comment) => {
                const canDelete = currentUserId && comment.author.id === currentUserId;

                return (
                  <div
                    key={comment.id}
                    style={{
                      backgroundColor: '#f8fafc',
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
                          }}
                        >
                          {(comment.author.name || 'U').charAt(0).toUpperCase()}
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
                  </div>
                );
              })}
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
