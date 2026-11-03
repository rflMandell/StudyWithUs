import React, { useEffect, useState } from 'react';
import { Trophy, Medal, Award, Flame } from 'lucide-react';
import { RankingEntry, UserRankingPosition } from '../types/ranking';
import { rankingService } from '../services/rankingService';
import { AlertError } from './AlertError';

interface RankingViewProps {
  communityId: string;
  isMember: boolean;
  currentUserId?: string;
}

export const RankingView: React.FC<RankingViewProps> = ({
  communityId,
  isMember,
  currentUserId,
}) => {
  const [ranking, setRanking] = useState<RankingEntry[]>([]);
  const [myPosition, setMyPosition] = useState<UserRankingPosition | null>(null);
  const [isLoading, setIsLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    const loadRankingData = async () => {
      setIsLoading(true);
      setError(null);
      try {
        const promises: [Promise<any>, Promise<any>?] = [
          rankingService.getCommunityRanking(communityId),
        ];

        if (isMember) {
          promises.push(rankingService.getMyRankingPosition(communityId));
        }

        const [rankingData, positionData] = await Promise.all(promises);
        setRanking(rankingData.content);
        if (positionData) {
          setMyPosition(positionData);
        }
      } catch (err) {
        setError('Não foi possível carregar o ranking da comunidade.');
      } finally {
        setIsLoading(false);
      }
    };

    loadRankingData();
  }, [communityId, isMember]);

  const getMedalColor = (pos: number) => {
    switch (pos) {
      case 1:
        return '#f59e0b'; // Gold
      case 2:
        return '#94a3b8'; // Silver
      case 3:
        return '#b45309'; // Bronze
      default:
        return 'var(--text-muted)';
    }
  };

  const top3 = ranking.slice(0, 3);

  return (
    <div>
      <AlertError message={error} />

      {isMember && myPosition && myPosition.position > 0 && (
        <div
          className="card"
          style={{
            marginBottom: '1.5rem',
            backgroundColor: '#f0fdf4',
            border: '1px solid #bbf7d0',
            display: 'flex',
            alignItems: 'center',
            justifyContent: 'space-between',
            flexWrap: 'wrap',
            gap: '1rem',
          }}
        >
          <div style={{ display: 'flex', alignItems: 'center', gap: '0.75rem' }}>
            <div
              style={{
                width: '42px',
                height: '42px',
                borderRadius: '50%',
                backgroundColor: '#dcfce7',
                color: '#16a34a',
                display: 'flex',
                alignItems: 'center',
                justifyContent: 'center',
              }}
            >
              <Trophy size={22} />
            </div>
            <div>
              <p style={{ fontWeight: 600, color: '#166534', fontSize: '1.05rem' }}>
                Sua Colocação Atual
              </p>
              <p style={{ color: '#15803d', fontSize: '0.875rem' }}>
                Você está no {myPosition.position}º lugar no ranking ativo da comunidade!
              </p>
            </div>
          </div>

          <div style={{ textAlign: 'right' }}>
            <span style={{ fontSize: '1.5rem', fontWeight: 700, color: '#166534' }}>
              {myPosition.points} <span style={{ fontSize: '0.9rem', fontWeight: 500 }}>pts</span>
            </span>
          </div>
        </div>
      )}

      {isLoading ? (
        <p style={{ textAlign: 'center', color: 'var(--text-muted)', padding: '2.5rem 0' }}>
          Carregando ranking da comunidade...
        </p>
      ) : ranking.length === 0 ? (
        <div className="card" style={{ textAlign: 'center', padding: '3rem 1.5rem' }}>
          <Trophy size={44} style={{ margin: '0 auto 0.75rem', color: 'var(--text-muted)' }} />
          <h3>Nenhum estudante classificado nesta semana</h3>
          <p style={{ color: 'var(--text-muted)', marginTop: '0.25rem', fontSize: '0.9rem' }}>
            {isMember
              ? 'Realize seus check-ins de estudo para pontuar e liderar o ranking!'
              : 'Participe da comunidade para competir com outros estudantes.'}
          </p>
        </div>
      ) : (
        <div>
          {top3.length >= 2 && (
            <div
              style={{
                display: 'grid',
                gridTemplateColumns: 'repeat(auto-fit, minmax(200px, 1fr))',
                gap: '1rem',
                marginBottom: '1.75rem',
              }}
            >
              {top3.map((entry) => (
                <div
                  key={entry.userId}
                  className="card"
                  style={{
                    textAlign: 'center',
                    padding: '1.5rem 1rem',
                    border: entry.position === 1 ? '2px solid #fde047' : '1px solid var(--border)',
                    backgroundColor: entry.position === 1 ? '#fefce8' : '#ffffff',
                  }}
                >
                  <div style={{ marginBottom: '0.5rem' }}>
                    {entry.position === 1 ? (
                      <Trophy size={36} color="#eab308" style={{ margin: '0 auto' }} />
                    ) : (
                      <Medal size={32} color={getMedalColor(entry.position)} style={{ margin: '0 auto' }} />
                    )}
                  </div>

                  <span
                    className="user-badge"
                    style={{
                      backgroundColor: entry.position === 1 ? '#fef08a' : '#f1f5f9',
                      color: entry.position === 1 ? '#854d0e' : '#475569',
                      fontWeight: 700,
                      marginBottom: '0.5rem',
                    }}
                  >
                    {entry.position}º Lugar
                  </span>

                  <h3 style={{ fontSize: '1.1rem', marginTop: '0.5rem', fontWeight: 600 }}>
                    {entry.name}
                  </h3>
                  <p style={{ fontSize: '0.85rem', color: 'var(--text-muted)', marginBottom: '0.75rem' }}>
                    @{entry.username}
                  </p>

                  <p style={{ fontSize: '1.4rem', fontWeight: 700, color: 'var(--primary)' }}>
                    {entry.points} <span style={{ fontSize: '0.85rem', fontWeight: 500 }}>pts</span>
                  </p>
                </div>
              ))}
            </div>
          )}

          <div className="card">
            <div style={{ display: 'flex', alignItems: 'center', gap: '0.5rem', marginBottom: '1.25rem' }}>
              <Award size={20} color="var(--primary)" />
              <h2 style={{ fontSize: '1.2rem', fontWeight: 600 }}>Tabela Geral de Classificação</h2>
            </div>

            <div style={{ display: 'flex', flexDirection: 'column', gap: '0.6rem' }}>
              {ranking.map((entry) => {
                const isCurrentUser = entry.userId === currentUserId;

                return (
                  <div
                    key={entry.userId}
                    style={{
                      display: 'flex',
                      alignItems: 'center',
                      justifyContent: 'space-between',
                      padding: '0.85rem 1rem',
                      backgroundColor: isCurrentUser ? '#eff6ff' : '#f8fafc',
                      borderRadius: '6px',
                      border: isCurrentUser ? '1px solid #bfdbfe' : '1px solid var(--border)',
                    }}
                  >
                    <div style={{ display: 'flex', alignItems: 'center', gap: '1rem' }}>
                      <span
                        style={{
                          width: '28px',
                          textAlign: 'center',
                          fontWeight: 700,
                          fontSize: '1rem',
                          color: getMedalColor(entry.position),
                        }}
                      >
                        {entry.position}º
                      </span>

                      <div>
                        <div style={{ display: 'flex', alignItems: 'center', gap: '0.4rem' }}>
                          <span style={{ fontWeight: 600 }}>{entry.name}</span>
                          {isCurrentUser && (
                            <span
                              style={{
                                fontSize: '0.75rem',
                                color: '#1d4ed8',
                                backgroundColor: '#dbeafe',
                                padding: '0.1rem 0.4rem',
                                borderRadius: '4px',
                                fontWeight: 600,
                              }}
                            >
                              Você
                            </span>
                          )}
                        </div>
                        <span style={{ fontSize: '0.8rem', color: 'var(--text-muted)' }}>
                          @{entry.username}
                        </span>
                      </div>
                    </div>

                    <div style={{ display: 'flex', alignItems: 'center', gap: '1.25rem' }}>
                      {entry.streakAtUpdate > 0 && (
                        <span
                          style={{
                            fontSize: '0.85rem',
                            color: '#ea580c',
                            display: 'flex',
                            alignItems: 'center',
                            gap: '0.2rem',
                          }}
                        >
                          <Flame size={14} />
                          {entry.streakAtUpdate}d
                        </span>
                      )}

                      <span style={{ fontWeight: 700, fontSize: '1.1rem', color: 'var(--primary)' }}>
                        {entry.points} pts
                      </span>
                    </div>
                  </div>
                );
              })}
            </div>
          </div>
        </div>
      )}
    </div>
  );
};
