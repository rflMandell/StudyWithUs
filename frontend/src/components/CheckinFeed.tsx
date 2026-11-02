import React, { useEffect, useState } from 'react';
import { Send, Trash2, Image, BookOpen, Clock } from 'lucide-react';
import { Checkin } from '../types/checkin';
import { CommunityRole, CommunityMember } from '../types/community';
import { checkinService } from '../services/checkinService';
import { AlertError } from './AlertError';

interface CheckinFeedProps {
  communityId: string;
  isMember: boolean;
  currentUserRole?: CommunityRole | null;
  currentUserId?: string;
  members: CommunityMember[];
}

export const CheckinFeed: React.FC<CheckinFeedProps> = ({
  communityId,
  isMember,
  currentUserRole,
  currentUserId,
  members,
}) => {
  const [checkins, setCheckins] = useState<Checkin[]>([]);
  const [caption, setCaption] = useState('');
  const [mediaUrl, setMediaUrl] = useState('');
  const [showMediaInput, setShowMediaInput] = useState(false);
  const [isLoading, setIsLoading] = useState(true);
  const [isSubmitting, setIsSubmitting] = useState(false);
  const [error, setError] = useState<string | null>(null);

  const isModeratorOrHigher =
    currentUserRole === 'OWNER' || currentUserRole === 'ADMIN' || currentUserRole === 'MODERATOR';

  const loadCheckins = async () => {
    setIsLoading(true);
    setError(null);
    try {
      const response = await checkinService.listCommunityCheckins(communityId);
      setCheckins(response.content);
    } catch (err: any) {
      setError('Não foi possível carregar os check-ins.');
    } finally {
      setIsLoading(false);
    }
  };

  useEffect(() => {
    loadCheckins();
  }, [communityId]);

  const handleCreateCheckin = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!caption.trim() && !mediaUrl.trim()) {
      setError('Adicione uma legenda ou mídia para seu check-in.');
      return;
    }

    setIsSubmitting(true);
    setError(null);

    try {
      const mediaKeys = mediaUrl.trim() ? [mediaUrl.trim()] : [];
      const idempotencyKey = crypto.randomUUID();

      await checkinService.createCheckin(
        {
          communityId,
          caption: caption.trim(),
          mediaKeys,
        },
        idempotencyKey
      );

      setCaption('');
      setMediaUrl('');
      setShowMediaInput(false);
      await loadCheckins();
    } catch (err: any) {
      const msg = err.response?.data?.message || err.response?.data?.code || 'Erro ao publicar check-in.';
      setError(msg);
    } finally {
      setIsSubmitting(false);
    }
  };

  const handleDeleteCheckin = async (checkinId: string) => {
    if (!confirm('Deseja realmente excluir este check-in?')) return;

    try {
      await checkinService.deleteCheckin(checkinId);
      await loadCheckins();
    } catch (err: any) {
      alert(err.response?.data?.message || 'Erro ao excluir check-in.');
    }
  };

  const getMemberDetails = (userId: string) => {
    return members.find((m) => m.userId === userId);
  };

  return (
    <div>
      <AlertError message={error} />

      {isMember && (
        <div className="card" style={{ marginBottom: '1.5rem', backgroundColor: '#ffffff' }}>
          <form onSubmit={handleCreateCheckin}>
            <div style={{ display: 'flex', alignItems: 'center', gap: '0.5rem', marginBottom: '0.75rem' }}>
              <BookOpen size={18} color="var(--primary)" />
              <h3 style={{ fontSize: '1.05rem', fontWeight: 600 }}>Registrar Estudo (Check-in)</h3>
            </div>

            <textarea
              className="form-control"
              rows={3}
              placeholder="O que você estudou hoje? Compartilhe seu progresso e tópicos aprendidos..."
              value={caption}
              onChange={(e) => setCaption(e.target.value)}
              disabled={isSubmitting}
              style={{ resize: 'vertical', marginBottom: '0.75rem' }}
            />

            {showMediaInput && (
              <div style={{ marginBottom: '0.75rem' }}>
                <input
                  type="text"
                  className="form-control"
                  placeholder="URL ou chave de armazenamento da foto/mídia..."
                  value={mediaUrl}
                  onChange={(e) => setMediaUrl(e.target.value)}
                  disabled={isSubmitting}
                />
              </div>
            )}

            <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
              <button
                type="button"
                onClick={() => setShowMediaInput(!showMediaInput)}
                className="btn btn-secondary"
                style={{ width: 'auto', padding: '0.4rem 0.8rem', fontSize: '0.85rem', gap: '0.3rem' }}
              >
                <Image size={16} />
                <span>{showMediaInput ? 'Remover Foto' : 'Adicionar Foto'}</span>
              </button>

              <button
                type="submit"
                disabled={isSubmitting}
                className="btn btn-primary"
                style={{ width: 'auto', padding: '0.5rem 1.25rem', gap: '0.4rem' }}
              >
                <Send size={16} />
                <span>{isSubmitting ? 'Publicando...' : 'Publicar Check-in'}</span>
              </button>
            </div>
          </form>
        </div>
      )}

      {isLoading ? (
        <p style={{ textAlign: 'center', color: 'var(--text-muted)', padding: '2rem 0' }}>Carregando check-ins...</p>
      ) : checkins.length === 0 ? (
        <div className="card" style={{ textAlign: 'center', padding: '2.5rem 1.5rem' }}>
          <BookOpen size={40} style={{ margin: '0 auto 0.75rem', color: 'var(--text-muted)' }} />
          <h3>Nenhum check-in registrado ainda</h3>
          <p style={{ color: 'var(--text-muted)', marginTop: '0.25rem', fontSize: '0.9rem' }}>
            {isMember
              ? 'Seja o primeiro a compartilhar seu progresso diário de estudos!'
              : 'Participe desta comunidade para poder realizar seus check-ins.'}
          </p>
        </div>
      ) : (
        <div style={{ display: 'flex', flexDirection: 'column', gap: '1rem' }}>
          {checkins.map((checkin) => {
            const author = getMemberDetails(checkin.userId);
            const canDelete = checkin.userId === currentUserId || isModeratorOrHigher;

            return (
              <div key={checkin.id} className="card" style={{ padding: '1.25rem' }}>
                <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'flex-start', marginBottom: '0.75rem' }}>
                  <div style={{ display: 'flex', alignItems: 'center', gap: '0.65rem' }}>
                    <div
                      style={{
                        width: '38px',
                        height: '38px',
                        borderRadius: '50%',
                        backgroundColor: '#e0e7ff',
                        color: '#4338ca',
                        display: 'flex',
                        alignItems: 'center',
                        justifyContent: 'center',
                        fontWeight: 700,
                        fontSize: '0.95rem',
                      }}
                    >
                      {(author?.name || 'A').charAt(0).toUpperCase()}
                    </div>
                    <div>
                      <p style={{ fontWeight: 600, fontSize: '0.95rem' }}>
                        {author ? author.name : `Estudante (${checkin.userId.substring(0, 8)})`}
                      </p>
                      <p style={{ fontSize: '0.8rem', color: 'var(--text-muted)', display: 'flex', alignItems: 'center', gap: '0.3rem' }}>
                        <Clock size={12} />
                        {new Date(checkin.createdAt).toLocaleString()}
                      </p>
                    </div>
                  </div>

                  {canDelete && (
                    <button
                      onClick={() => handleDeleteCheckin(checkin.id)}
                      className="btn btn-secondary"
                      title="Excluir check-in"
                      style={{ padding: '0.35rem 0.5rem', width: 'auto', color: '#dc2626' }}
                    >
                      <Trash2 size={16} />
                    </button>
                  )}
                </div>

                {checkin.caption && (
                  <p style={{ fontSize: '0.95rem', lineHeight: '1.5', marginBottom: '0.75rem', whiteSpace: 'pre-wrap' }}>
                    {checkin.caption}
                  </p>
                )}

                {checkin.mediaKeys && checkin.mediaKeys.length > 0 && (
                  <div style={{ display: 'flex', gap: '0.5rem', flexWrap: 'wrap', marginTop: '0.5rem' }}>
                    {checkin.mediaKeys.map((key, idx) => (
                      <div
                        key={idx}
                        style={{
                          backgroundColor: '#f1f5f9',
                          padding: '0.5rem 0.75rem',
                          borderRadius: '6px',
                          fontSize: '0.85rem',
                          color: '#334155',
                          display: 'flex',
                          alignItems: 'center',
                          gap: '0.4rem',
                        }}
                      >
                        <Image size={14} />
                        <span>{key}</span>
                      </div>
                    ))}
                  </div>
                )}
              </div>
            );
          })}
        </div>
      )}
    </div>
  );
};
