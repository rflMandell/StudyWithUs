import React, { useEffect, useState } from 'react';
import { Link } from 'react-router-dom';
import { Newspaper, Users, Clock, Award, Image, RefreshCw, ChevronDown } from 'lucide-react';
import { FeedItem } from '../types/feed';
import { feedService } from '../services/feedService';
import { useAuth } from '../hooks/useAuth';
import { CommentSection } from './CommentSection';
import { getAvatarBorderStyle } from '../utils/levelCosmetics';

export const ActivityFeed: React.FC = () => {
  const { user } = useAuth();
  const [items, setItems] = useState<FeedItem[]>([]);
  const [nextCursor, setNextCursor] = useState<string | null>(null);
  const [hasMore, setHasMore] = useState(false);
  const [isLoading, setIsLoading] = useState(true);
  const [isLoadingMore, setIsLoadingMore] = useState(false);
  const [error, setError] = useState<string | null>(null);

  const loadInitialFeed = async () => {
    setIsLoading(true);
    setError(null);
    try {
      const response = await feedService.getFeed(null, 10);
      setItems(response.content);
      setNextCursor(response.nextCursor || null);
      setHasMore(response.hasMore);
    } catch (err: any) {
      console.error('Erro ao carregar feed:', err);
      setError('Não foi possível carregar o feed de atividades no momento.');
    } finally {
      setIsLoading(false);
    }
  };

  const handleLoadMore = async () => {
    if (!nextCursor || isLoadingMore) return;
    setIsLoadingMore(true);
    try {
      const response = await feedService.getFeed(nextCursor, 10);
      setItems((prev) => [...prev, ...response.content]);
      setNextCursor(response.nextCursor || null);
      setHasMore(response.hasMore);
    } catch (err: any) {
      console.error('Erro ao carregar mais itens:', err);
    } finally {
      setIsLoadingMore(false);
    }
  };

  useEffect(() => {
    loadInitialFeed();
  }, []);

  return (
    <div className="card" style={{ marginBottom: '2rem' }}>
      <div
        style={{
          display: 'flex',
          alignItems: 'center',
          justifyContent: 'space-between',
          marginBottom: '1.25rem',
        }}
      >
        <div style={{ display: 'flex', alignItems: 'center', gap: '0.6rem' }}>
          <Newspaper size={22} color="var(--primary)" />
          <h2 style={{ fontSize: '1.25rem', fontWeight: 600 }}>Feed de Atividades dos Seus Grupos</h2>
        </div>
        <button
          onClick={loadInitialFeed}
          disabled={isLoading}
          style={{
            background: 'none',
            border: 'none',
            color: 'var(--text-muted)',
            cursor: 'pointer',
            display: 'flex',
            alignItems: 'center',
            gap: '0.35rem',
            fontSize: '0.85rem',
          }}
          title="Atualizar feed"
        >
          <RefreshCw size={14} className={isLoading ? 'spin' : ''} />
          <span>Atualizar</span>
        </button>
      </div>

      {isLoading ? (
        <p style={{ color: 'var(--text-muted)', fontSize: '0.9rem', textAlign: 'center', padding: '2rem 0' }}>
          Carregando atividades dos grupos...
        </p>
      ) : error ? (
        <div
          style={{
            padding: '1rem',
            backgroundColor: '#fef2f2',
            color: '#dc2626',
            borderRadius: '6px',
            fontSize: '0.9rem',
            textAlign: 'center',
          }}
        >
          {error}
        </div>
      ) : items.length === 0 ? (
        <div style={{ textAlign: 'center', padding: '2rem 1rem' }}>
          <p style={{ color: 'var(--text-muted)', fontSize: '0.95rem', marginBottom: '0.75rem' }}>
            Nenhuma atividade recente encontrada nas suas comunidades.
          </p>
          <p style={{ color: 'var(--text-muted)', fontSize: '0.85rem', marginBottom: '1.25rem' }}>
            Participe de comunidades e faça check-ins para acompanhar os estudos dos colegas!
          </p>
          <Link
            to="/communities"
            className="btn btn-primary"
            style={{ display: 'inline-flex', alignItems: 'center', gap: '0.4rem', fontSize: '0.9rem' }}
          >
            <Users size={16} />
            Explorar Comunidades
          </Link>
        </div>
      ) : (
        <div style={{ display: 'flex', flexDirection: 'column', gap: '1rem' }}>
          {items.map((item) => (
            <div
              key={item.id}
              style={{
                border: '1px solid var(--border)',
                borderRadius: '8px',
                padding: '1rem',
                backgroundColor: 'var(--bg-card)',
                boxShadow: '0 1px 2px rgba(0,0,0,0.03)',
              }}
            >
              {/* Header: Author + Community */}
              <div
                style={{
                  display: 'flex',
                  alignItems: 'flex-start',
                  justifyContent: 'space-between',
                  marginBottom: '0.75rem',
                }}
              >
                <div style={{ display: 'flex', alignItems: 'center', gap: '0.65rem' }}>
                  <div
                    style={{
                      width: '38px',
                      height: '38px',
                      borderRadius: '50%',
                      backgroundColor: 'var(--primary-subtle, #eff6ff)',
                      color: 'var(--primary)',
                      display: 'flex',
                      alignItems: 'center',
                      justifyContent: 'center',
                      fontWeight: 700,
                      fontSize: '0.95rem',
                      overflow: 'hidden',
                      border: getAvatarBorderStyle(item.authorAvatarBorder),
                      flexShrink: 0,
                    }}
                  >
                    {item.authorAvatarUrl ? (
                      <img
                        src={item.authorAvatarUrl}
                        alt={item.authorName || 'Avatar'}
                        style={{ width: '100%', height: '100%', objectFit: 'cover' }}
                      />
                    ) : (
                      (item.authorName || item.authorUsername || 'U').charAt(0).toUpperCase()
                    )}
                  </div>
                  <div>
                    <div style={{ display: 'flex', alignItems: 'center', gap: '0.4rem', flexWrap: 'wrap' }}>
                      <span style={{ fontWeight: 600, fontSize: '0.95rem', color: 'var(--text-main)' }}>
                        {item.authorName || 'Estudante'}
                      </span>
                      {item.authorLevel != null && (
                        <span
                          style={{
                            fontSize: '0.7rem',
                            fontWeight: 700,
                            backgroundColor: 'var(--primary-light)',
                            color: 'var(--primary)',
                            padding: '0.1rem 0.45rem',
                            borderRadius: '9999px',
                          }}
                        >
                          Nv. {item.authorLevel}
                        </span>
                      )}
                      {item.authorTitle && (
                        <span style={{ fontSize: '0.75rem', color: '#6366f1', fontWeight: 500 }}>
                          {item.authorTitle}
                        </span>
                      )}
                      {item.authorUsername && (
                        <span style={{ fontSize: '0.8rem', color: 'var(--text-muted)' }}>
                          @{item.authorUsername}
                        </span>
                      )}
                    </div>
                    <div style={{ display: 'flex', alignItems: 'center', gap: '0.4rem', marginTop: '0.15rem' }}>
                      <Link
                        to={`/communities/${item.communityId}`}
                        style={{
                          fontSize: '0.75rem',
                          fontWeight: 500,
                          backgroundColor: 'var(--bg-secondary)',
                          color: 'var(--text-muted)',
                          padding: '0.1rem 0.45rem',
                          borderRadius: '4px',
                          textDecoration: 'none',
                          display: 'inline-flex',
                          alignItems: 'center',
                          gap: '0.25rem',
                        }}
                      >
                        <Users size={11} />
                        {item.communityName || 'Comunidade'}
                      </Link>
                      <span style={{ color: 'var(--text-muted)', fontSize: '0.75rem' }}>•</span>
                      <span
                        style={{
                          fontSize: '0.75rem',
                          color: 'var(--text-muted)',
                          display: 'inline-flex',
                          alignItems: 'center',
                          gap: '0.25rem',
                        }}
                      >
                        <Clock size={11} />
                        {new Date(item.createdAt).toLocaleString()}
                      </span>
                    </div>
                  </div>
                </div>

                <div
                  style={{
                    display: 'flex',
                    alignItems: 'center',
                    gap: '0.3rem',
                    color: 'var(--success)',
                    backgroundColor: 'var(--success-light)',
                    fontSize: '0.75rem',
                    fontWeight: 600,
                    padding: '0.2rem 0.5rem',
                    borderRadius: '9999px',
                  }}
                >
                  <Award size={13} />
                  <span>+10 pts</span>
                </div>
              </div>

              {/* Caption */}
              {item.caption && (
                <p
                  style={{
                    fontSize: '0.95rem',
                    color: 'var(--text-main)',
                    lineHeight: 1.5,
                    marginBottom: item.mediaKeys && item.mediaKeys.length > 0 ? '0.75rem' : 0,
                    whiteSpace: 'pre-line',
                  }}
                >
                  {item.caption}
                </p>
              )}

              {/* Media preview */}
              {item.mediaUrls && item.mediaUrls.length > 0 ? (
                <div style={{ display: 'flex', gap: '0.5rem', flexWrap: 'wrap', marginTop: '0.5rem' }}>
                  {item.mediaUrls.map((url, idx) => (
                    <img
                      key={idx}
                      src={url}
                      alt={`Foto do check-in ${idx + 1}`}
                      style={{
                        maxHeight: '320px',
                        maxWidth: '100%',
                        borderRadius: '8px',
                        border: '1px solid var(--border)',
                        objectFit: 'cover',
                      }}
                    />
                  ))}
                </div>
              ) : (
                item.mediaKeys &&
                item.mediaKeys.length > 0 && (
                  <div
                    style={{
                      marginTop: '0.5rem',
                      padding: '0.5rem 0.75rem',
                      backgroundColor: 'var(--bg-main)',
                      borderRadius: '6px',
                      border: '1px solid var(--border)',
                      display: 'flex',
                      alignItems: 'center',
                      gap: '0.5rem',
                      fontSize: '0.8rem',
                      color: 'var(--text-muted)',
                    }}
                  >
                    <Image size={16} />
                    <span>{item.mediaKeys.length} anexo(s) incluído(s)</span>
                  </div>
                )
              )}

              <CommentSection
                checkinId={item.id}
                initialCommentsCount={item.commentsCount || 0}
                initialLikesCount={item.likesCount || 0}
                initialLikedByCurrentUser={Boolean(item.likedByCurrentUser)}
                isMember={true}
                currentUserId={user?.id}
              />
            </div>
          ))}

          {/* Load more button */}
          {hasMore && (
            <div style={{ textAlign: 'center', marginTop: '0.5rem' }}>
              <button
                onClick={handleLoadMore}
                disabled={isLoadingMore}
                className="btn btn-secondary"
                style={{
                  display: 'inline-flex',
                  alignItems: 'center',
                  gap: '0.4rem',
                  fontSize: '0.9rem',
                }}
              >
                <ChevronDown size={16} />
                {isLoadingMore ? 'Carregando mais...' : 'Carregar mais atividades'}
              </button>
            </div>
          )}
        </div>
      )}
    </div>
  );
};
