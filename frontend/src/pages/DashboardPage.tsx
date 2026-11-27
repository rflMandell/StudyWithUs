import React, { useCallback, useEffect, useState } from 'react';
import { Award, School, Sparkles, CheckCircle2, History, ArrowUpRight, ArrowDownLeft, Clock, PenSquare } from 'lucide-react';
import { useAuth } from '../hooks/useAuth';
import { Navbar } from '../components/Navbar';
import { ActivityFeed } from '../components/ActivityFeed';
import { CreatePostModal } from '../components/CreatePostModal';
import { PointTransaction, UserPointsSummary } from '../types/points';
import { pointsService } from '../services/pointsService';

export const DashboardPage: React.FC = () => {
  const { user, refreshUser } = useAuth();
  const [pointsSummary, setPointsSummary] = useState<UserPointsSummary | null>(null);
  const [transactions, setTransactions] = useState<PointTransaction[]>([]);
  const [isLoadingPoints, setIsLoadingPoints] = useState(true);
  const [isCreatePostOpen, setIsCreatePostOpen] = useState(false);
  const [feedRefreshKey, setFeedRefreshKey] = useState(0);

  const fetchPoints = useCallback(async () => {
    try {
      const [summary, history] = await Promise.all([
        pointsService.getPointsSummary(),
        pointsService.getPointsHistory(0, 10),
      ]);
      setPointsSummary(summary);
      setTransactions(history.content);
    } catch (err) {
      console.error('Erro ao carregar dados de pontos:', err);
    } finally {
      setIsLoadingPoints(false);
    }
  }, []);

  useEffect(() => {
    fetchPoints();
  }, [fetchPoints]);

  const handlePostCreated = async () => {
    setIsCreatePostOpen(false);
    setFeedRefreshKey((prev) => prev + 1);
    await Promise.all([fetchPoints(), refreshUser?.()]);
  };

  if (!user) return null;

  return (
    <div>
      <Navbar />
      <main className="container">
        <div
          style={{
            marginBottom: '2rem',
            display: 'flex',
            alignItems: 'center',
            justifyContent: 'space-between',
            flexWrap: 'wrap',
            gap: '1rem',
          }}
        >
          <div>
            <h1 style={{ fontSize: '2rem', fontWeight: 700, letterSpacing: '-0.025em' }}>
              Olá, {user.name}! 👋
            </h1>
            <p style={{ color: 'var(--text-muted)', marginTop: '0.25rem' }}>
              Bem-vindo(a) ao StudyWithUs. Sua consistência acadêmica começa aqui.
            </p>
          </div>
          <button
            type="button"
            onClick={() => setIsCreatePostOpen(true)}
            className="btn btn-primary"
            style={{ width: 'auto', padding: '0.65rem 1.25rem', gap: '0.5rem' }}
          >
            <PenSquare size={18} />
            <span>Novo Post</span>
          </button>
        </div>

        <div className="grid-cards" style={{ marginBottom: '2rem' }}>
          <div className="card">
            <div style={{ display: 'flex', alignItems: 'center', gap: '0.6rem', color: 'var(--primary)', marginBottom: '0.5rem' }}>
              <Award size={22} />
              <h3>Pontuação Atual</h3>
            </div>
            <p style={{ fontSize: '2.25rem', fontWeight: 700, color: 'var(--primary)' }}>
              {isLoadingPoints ? '...' : (pointsSummary?.currentPoints ?? user.lifetimePoints)}{' '}
              <span style={{ fontSize: '1rem', fontWeight: 500, color: 'var(--text-muted)' }}>pontos</span>
            </p>
            <p style={{ fontSize: '0.85rem', color: 'var(--text-muted)', marginTop: '0.5rem' }}>
              Total vitalício acumulado: {isLoadingPoints ? '...' : (pointsSummary?.lifetimePoints ?? user.lifetimePoints)} pts
            </p>
          </div>

          <div className="card">
            <div style={{ display: 'flex', alignItems: 'center', gap: '0.6rem', color: '#0284c7', marginBottom: '0.5rem' }}>
              <School size={22} />
              <h3>Dados Acadêmicos</h3>
            </div>
            <p style={{ fontWeight: 600 }}>{user.institution || 'Instituição não informada'}</p>
            <p style={{ color: 'var(--text-muted)', fontSize: '0.9rem' }}>{user.course || 'Curso não informado'}</p>
            <div style={{ marginTop: '0.75rem', display: 'flex', alignItems: 'center', gap: '0.4rem', color: '#16a34a', fontSize: '0.85rem', fontWeight: 500 }}>
              <CheckCircle2 size={16} />
              <span>Conta Verificada e Ativa</span>
            </div>
          </div>

          <div className="card">
            <div style={{ display: 'flex', alignItems: 'center', gap: '0.6rem', color: '#d97706', marginBottom: '0.5rem' }}>
              <Sparkles size={22} />
              <h3>Como pontuar</h3>
            </div>
            <ul style={{ listStyle: 'none', fontSize: '0.9rem', color: 'var(--text-muted)', display: 'flex', flexDirection: 'column', gap: '0.4rem' }}>
              <li>• Realize check-ins diários (+10 pts)</li>
              <li>• Mantenha sequências de estudo (Streaks)</li>
              <li>• Conclua desafios de comunidades acadêmicas</li>
            </ul>
          </div>
        </div>

        <ActivityFeed refreshKey={feedRefreshKey} />

        <div className="card">
          <div style={{ display: 'flex', alignItems: 'center', gap: '0.5rem', marginBottom: '1.25rem' }}>
            <History size={20} color="var(--primary)" />
            <h2 style={{ fontSize: '1.25rem', fontWeight: 600 }}>Extrato de Pontos</h2>
          </div>

          {isLoadingPoints ? (
            <p style={{ color: 'var(--text-muted)', fontSize: '0.9rem', textAlign: 'center', padding: '1.5rem 0' }}>
              Carregando histórico de transações...
            </p>
          ) : transactions.length === 0 ? (
            <p style={{ color: 'var(--text-muted)', fontSize: '0.9rem', textAlign: 'center', padding: '1.5rem 0' }}>
              Nenhuma transação de pontos registrada ainda. Faça um check-in em uma comunidade para pontuar!
            </p>
          ) : (
            <div style={{ display: 'flex', flexDirection: 'column', gap: '0.75rem' }}>
              {transactions.map((tx) => {
                const isPositive = tx.amount > 0;

                return (
                  <div
                    key={tx.id}
                    style={{
                      display: 'flex',
                      alignItems: 'center',
                      justifyContent: 'space-between',
                      padding: '0.75rem 1rem',
                      backgroundColor: 'var(--bg-main)',
                      borderRadius: '6px',
                      border: '1px solid var(--border)',
                    }}
                  >
                    <div style={{ display: 'flex', alignItems: 'center', gap: '0.75rem' }}>
                      <div
                        style={{
                          width: '36px',
                          height: '36px',
                          borderRadius: '50%',
                          backgroundColor: isPositive ? '#ecfdf5' : '#fef2f2',
                          color: isPositive ? '#059669' : '#dc2626',
                          display: 'flex',
                          alignItems: 'center',
                          justifyContent: 'center',
                        }}
                      >
                        {isPositive ? <ArrowUpRight size={18} /> : <ArrowDownLeft size={18} />}
                      </div>

                      <div>
                        <div style={{ display: 'flex', alignItems: 'center', gap: '0.4rem' }}>
                          <span style={{ fontWeight: 600, fontSize: '0.95rem' }}>
                            {tx.type === 'CHECKIN'
                              ? 'Check-in Realizado'
                              : tx.type === 'REVERSAL'
                              ? 'Estorno de Check-in'
                              : tx.type}
                          </span>
                          {tx.reversedAt && (
                            <span
                              style={{
                                fontSize: '0.75rem',
                                color: '#b45309',
                                backgroundColor: '#fef3c7',
                                padding: '0.15rem 0.4rem',
                                borderRadius: '4px',
                              }}
                            >
                              Estornado
                            </span>
                          )}
                        </div>
                        <p style={{ fontSize: '0.8rem', color: 'var(--text-muted)', display: 'flex', alignItems: 'center', gap: '0.3rem' }}>
                          <Clock size={12} />
                          {new Date(tx.createdAt).toLocaleString()}
                        </p>
                      </div>
                    </div>

                    <div style={{ textAlign: 'right' }}>
                      <span
                        style={{
                          fontWeight: 700,
                          fontSize: '1.05rem',
                          color: isPositive ? '#059669' : '#dc2626',
                        }}
                      >
                        {isPositive ? `+${tx.amount}` : tx.amount} pts
                      </span>
                    </div>
                  </div>
                );
              })}
            </div>
          )}
        </div>
      </main>
      <CreatePostModal
        isOpen={isCreatePostOpen}
        onClose={() => setIsCreatePostOpen(false)}
        onCreated={handlePostCreated}
      />
    </div>
  );
};
