import React, { useEffect, useState } from 'react';
import { useParams, Link } from 'react-router-dom';
import { ArrowLeft, Users, Globe, Lock, ShieldCheck, UserPlus, LogOut } from 'lucide-react';
import { Community, CommunityMember } from '../types/community';
import { communityService } from '../services/communityService';
import { Navbar } from '../components/Navbar';
import { AlertError } from '../components/AlertError';

export const CommunityDetailPage: React.FC = () => {
  const { id } = useParams<{ id: string }>();
  const [community, setCommunity] = useState<Community | null>(null);
  const [members, setMembers] = useState<CommunityMember[]>([]);
  const [isLoading, setIsLoading] = useState(true);
  const [actionLoading, setActionLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);

  const loadData = async (commId: string) => {
    setIsLoading(true);
    setError(null);
    try {
      const [commData, membersData] = await Promise.all([
        communityService.getCommunityDetails(commId),
        communityService.listMembers(commId),
      ]);
      setCommunity(commData);
      setMembers(membersData.content);
    } catch (err: any) {
      setError('Não foi possível carregar os detalhes da comunidade.');
    } finally {
      setIsLoading(false);
    }
  };

  useEffect(() => {
    if (id) {
      loadData(id);
    }
  }, [id]);

  const handleJoin = async () => {
    if (!community) return;
    setActionLoading(true);
    try {
      await communityService.joinCommunity(community.id);
      await loadData(community.id);
    } catch (err: any) {
      const msg = err.response?.data?.message || err.response?.data?.code || 'Erro ao participar.';
      alert(msg);
    } finally {
      setActionLoading(false);
    }
  };

  const handleLeave = async () => {
    if (!community) return;
    if (!confirm('Deseja realmente sair desta comunidade?')) return;

    setActionLoading(true);
    try {
      await communityService.leaveCommunity(community.id);
      await loadData(community.id);
    } catch (err: any) {
      const msg = err.response?.data?.message || err.response?.data?.code || 'Erro ao sair.';
      alert(msg);
    } finally {
      setActionLoading(false);
    }
  };

  return (
    <div>
      <Navbar />
      <main className="container">
        <Link
          to="/communities"
          style={{ display: 'inline-flex', alignItems: 'center', gap: '0.4rem', color: 'var(--text-muted)', marginBottom: '1.5rem', fontSize: '0.9rem' }}
        >
          <ArrowLeft size={16} />
          <span>Voltar para comunidades</span>
        </Link>

        <AlertError message={error} />

        {isLoading ? (
          <p style={{ textAlign: 'center', color: 'var(--text-muted)', padding: '3rem 0' }}>Carregando detalhes...</p>
        ) : community ? (
          <div>
            <div className="card" style={{ marginBottom: '2rem' }}>
              <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'flex-start', flexWrap: 'wrap', gap: '1rem' }}>
                <div>
                  <div style={{ display: 'flex', alignItems: 'center', gap: '0.5rem', marginBottom: '0.5rem' }}>
                    <span
                      className="user-badge"
                      style={{
                        display: 'inline-flex',
                        alignItems: 'center',
                        gap: '0.3rem',
                        backgroundColor: community.visibility === 'PUBLIC' ? '#ecfdf5' : '#fef3c7',
                        color: community.visibility === 'PUBLIC' ? '#065f46' : '#92400e',
                      }}
                    >
                      {community.visibility === 'PUBLIC' ? <Globe size={12} /> : <Lock size={12} />}
                      {community.visibility === 'PUBLIC' ? 'Pública' : 'Privada'}
                    </span>

                    <span style={{ fontSize: '0.85rem', color: 'var(--text-muted)' }}>
                      Fuso: {community.timezone}
                    </span>
                  </div>

                  <h1 style={{ fontSize: '1.85rem', fontWeight: 700 }}>{community.name}</h1>
                  <p style={{ color: 'var(--text-muted)', marginTop: '0.4rem', maxWidth: '700px' }}>
                    {community.description || 'Sem descrição.'}
                  </p>
                </div>

                <div>
                  {!community.currentUserRole ? (
                    community.visibility === 'PUBLIC' ? (
                      <button
                        onClick={handleJoin}
                        disabled={actionLoading}
                        className="btn btn-primary"
                        style={{ width: 'auto', gap: '0.4rem' }}
                      >
                        <UserPlus size={18} />
                        <span>{actionLoading ? 'Entrando...' : 'Participar da Comunidade'}</span>
                      </button>
                    ) : (
                      <span className="user-badge">Comunidade Privada</span>
                    )
                  ) : (
                    <div style={{ display: 'flex', alignItems: 'center', gap: '0.75rem' }}>
                      <span className="user-badge" style={{ backgroundColor: '#ecfdf5', color: '#065f46', fontSize: '0.9rem', padding: '0.4rem 0.8rem' }}>
                        Papel: {community.currentUserRole}
                      </span>

                      {community.currentUserRole !== 'OWNER' && (
                        <button
                          onClick={handleLeave}
                          disabled={actionLoading}
                          className="btn btn-secondary"
                          style={{ width: 'auto', gap: '0.3rem', color: '#dc2626' }}
                        >
                          <LogOut size={16} />
                          <span>Sair</span>
                        </button>
                      )}
                    </div>
                  )}
                </div>
              </div>
            </div>

            <div className="card">
              <div style={{ display: 'flex', alignItems: 'center', gap: '0.5rem', marginBottom: '1.25rem' }}>
                <Users size={20} color="var(--primary)" />
                <h2 style={{ fontSize: '1.25rem', fontWeight: 600 }}>Membros ({community.membersCount})</h2>
              </div>

              {members.length === 0 ? (
                <p style={{ color: 'var(--text-muted)', fontSize: '0.9rem' }}>Nenhum membro listado.</p>
              ) : (
                <div style={{ display: 'flex', flexDirection: 'column', gap: '0.75rem' }}>
                  {members.map((member) => (
                    <div
                      key={member.userId}
                      style={{
                        display: 'flex',
                        alignItems: 'center',
                        justifyContent: 'space-between',
                        padding: '0.75rem 1rem',
                        backgroundColor: '#f8fafc',
                        borderRadius: '6px',
                        border: '1px solid var(--border)',
                      }}
                    >
                      <div>
                        <p style={{ fontWeight: 600 }}>{member.name}</p>
                        <p style={{ fontSize: '0.85rem', color: 'var(--text-muted)' }}>@{member.username}</p>
                      </div>

                      <span
                        className="user-badge"
                        style={{
                          backgroundColor: member.role === 'OWNER' ? '#fef3c7' : '#eef2ff',
                          color: member.role === 'OWNER' ? '#92400e' : '#4338ca',
                          display: 'inline-flex',
                          alignItems: 'center',
                          gap: '0.3rem',
                        }}
                      >
                        {member.role === 'OWNER' && <ShieldCheck size={14} />}
                        {member.role}
                      </span>
                    </div>
                  ))}
                </div>
              )}
            </div>
          </div>
        ) : null}
      </main>
    </div>
  );
};
