import React, { useEffect, useState } from 'react';
import { useParams, Link } from 'react-router-dom';
import {
  ArrowLeft,
  Users,
  Globe,
  Lock,
  ShieldCheck,
  UserPlus,
  LogOut,
  Check,
  X,
  UserMinus,
  Settings,
  BookOpen,
} from 'lucide-react';
import { Community, CommunityJoinRequest, CommunityMember, CommunityRole } from '../types/community';
import { communityService } from '../services/communityService';
import { Navbar } from '../components/Navbar';
import { AlertError } from '../components/AlertError';
import { CheckinFeed } from '../components/CheckinFeed';
import { useAuth } from '../hooks/useAuth';

export const CommunityDetailPage: React.FC = () => {
  const { id } = useParams<{ id: string }>();
  const { user } = useAuth();

  const [community, setCommunity] = useState<Community | null>(null);
  const [members, setMembers] = useState<CommunityMember[]>([]);
  const [joinRequests, setJoinRequests] = useState<CommunityJoinRequest[]>([]);
  const [hasPendingRequest, setHasPendingRequest] = useState(false);
  const [activeTab, setActiveTab] = useState<'feed' | 'members' | 'requests'>('feed');

  const [isLoading, setIsLoading] = useState(true);
  const [actionLoading, setActionLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);

  const isOwner = community?.currentUserRole === 'OWNER';
  const isAdmin = community?.currentUserRole === 'ADMIN' || isOwner;
  const isMember = !!community?.currentUserRole;

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

      if (commData.currentUserRole === 'OWNER' || commData.currentUserRole === 'ADMIN') {
        const requestsData = await communityService.listJoinRequests(commId);
        setJoinRequests(requestsData.content);
      }
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
    setError(null);
    try {
      await communityService.joinCommunity(community.id);
      await loadData(community.id);
    } catch (err: any) {
      const msg = err.response?.data?.message || err.response?.data?.code || 'Erro ao participar da comunidade.';
      setError(msg);
    } finally {
      setActionLoading(false);
    }
  };

  const handleRequestJoin = async () => {
    if (!community) return;
    setActionLoading(true);
    setError(null);
    try {
      await communityService.requestJoin(community.id);
      setHasPendingRequest(true);
      alert('Solicitação de entrada enviada com sucesso! Aguarde a aprovação de um administrador.');
    } catch (err: any) {
      const msg = err.response?.data?.message || err.response?.data?.code || 'Erro ao solicitar entrada.';
      if (err.response?.data?.code === 'JOIN_REQUEST_ALREADY_PENDING') {
        setHasPendingRequest(true);
      }
      setError(msg);
    } finally {
      setActionLoading(false);
    }
  };

  const handleLeave = async () => {
    if (!community) return;
    if (!confirm('Deseja realmente sair desta comunidade?')) return;

    setActionLoading(true);
    setError(null);
    try {
      await communityService.leaveCommunity(community.id);
      await loadData(community.id);
    } catch (err: any) {
      const msg = err.response?.data?.message || err.response?.data?.code || 'Erro ao sair da comunidade.';
      setError(msg);
    } finally {
      setActionLoading(false);
    }
  };

  const handleApproveRequest = async (requestId: string) => {
    if (!community) return;
    setActionLoading(true);
    try {
      await communityService.approveJoinRequest(community.id, requestId);
      await loadData(community.id);
    } catch (err: any) {
      alert(err.response?.data?.message || 'Erro ao aprovar solicitação.');
    } finally {
      setActionLoading(false);
    }
  };

  const handleRejectRequest = async (requestId: string) => {
    if (!community) return;
    setActionLoading(true);
    try {
      await communityService.rejectJoinRequest(community.id, requestId);
      await loadData(community.id);
    } catch (err: any) {
      alert(err.response?.data?.message || 'Erro ao rejeitar solicitação.');
    } finally {
      setActionLoading(false);
    }
  };

  const handleUpdateRole = async (targetUserId: string, newRole: CommunityRole) => {
    if (!community) return;
    if (newRole === 'OWNER' && !confirm('Transferir a posse da comunidade para outro usuário tornará você um administrador. Deseja continuar?')) {
      return;
    }

    setActionLoading(true);
    try {
      await communityService.updateMemberRole(community.id, targetUserId, newRole);
      await loadData(community.id);
    } catch (err: any) {
      alert(err.response?.data?.message || 'Erro ao alterar papel.');
    } finally {
      setActionLoading(false);
    }
  };

  const handleRemoveMember = async (targetUserId: string, memberName: string) => {
    if (!community) return;
    if (!confirm(`Deseja remover o membro "${memberName}" da comunidade?`)) return;

    setActionLoading(true);
    try {
      await communityService.removeMember(community.id, targetUserId);
      await loadData(community.id);
    } catch (err: any) {
      alert(err.response?.data?.message || 'Erro ao remover membro.');
    } finally {
      setActionLoading(false);
    }
  };

  const canManageMember = (targetMember: CommunityMember) => {
    if (!isAdmin) return false;
    if (targetMember.userId === user?.id) return false;
    if (targetMember.role === 'OWNER') return false;
    if (community?.currentUserRole === 'ADMIN' && targetMember.role === 'ADMIN') return false;
    return true;
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
                    ) : hasPendingRequest ? (
                      <span className="user-badge" style={{ backgroundColor: '#fef3c7', color: '#92400e', padding: '0.5rem 0.8rem', fontSize: '0.9rem' }}>
                        Solicitação Enviada (Pendente)
                      </span>
                    ) : (
                      <button
                        onClick={handleRequestJoin}
                        disabled={actionLoading}
                        className="btn btn-primary"
                        style={{ width: 'auto', gap: '0.4rem', backgroundColor: '#d97706' }}
                      >
                        <Lock size={18} />
                        <span>{actionLoading ? 'Solicitando...' : 'Solicitar Entrada'}</span>
                      </button>
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

            <div style={{ display: 'flex', gap: '1rem', marginBottom: '1.25rem', borderBottom: '1px solid var(--border)', paddingBottom: '0.5rem', flexWrap: 'wrap' }}>
              <button
                onClick={() => setActiveTab('feed')}
                className="btn"
                style={{
                  width: 'auto',
                  backgroundColor: activeTab === 'feed' ? 'var(--primary)' : 'transparent',
                  color: activeTab === 'feed' ? '#fff' : 'var(--text-muted)',
                  border: 'none',
                  padding: '0.5rem 1rem',
                  fontSize: '0.9rem',
                  fontWeight: 600,
                }}
              >
                <BookOpen size={16} style={{ marginRight: '0.4rem', verticalAlign: 'middle' }} />
                Check-ins & Feed
              </button>

              <button
                onClick={() => setActiveTab('members')}
                className="btn"
                style={{
                  width: 'auto',
                  backgroundColor: activeTab === 'members' ? 'var(--primary)' : 'transparent',
                  color: activeTab === 'members' ? '#fff' : 'var(--text-muted)',
                  border: 'none',
                  padding: '0.5rem 1rem',
                  fontSize: '0.9rem',
                  fontWeight: 600,
                }}
              >
                <Users size={16} style={{ marginRight: '0.4rem', verticalAlign: 'middle' }} />
                Membros ({community.membersCount})
              </button>

              {isAdmin && (
                <button
                  onClick={() => setActiveTab('requests')}
                  className="btn"
                  style={{
                    width: 'auto',
                    backgroundColor: activeTab === 'requests' ? 'var(--primary)' : 'transparent',
                    color: activeTab === 'requests' ? '#fff' : 'var(--text-muted)',
                    border: 'none',
                    padding: '0.5rem 1rem',
                    fontSize: '0.9rem',
                    fontWeight: 600,
                  }}
                >
                  <Settings size={16} style={{ marginRight: '0.4rem', verticalAlign: 'middle' }} />
                  Solicitações Pendentes ({joinRequests.length})
                </button>
              )}
            </div>

            {activeTab === 'feed' && (
              <CheckinFeed
                communityId={community.id}
                isMember={isMember}
                currentUserRole={community.currentUserRole}
                currentUserId={user?.id}
                members={members}
              />
            )}

            {activeTab === 'requests' && isAdmin && (
              <div className="card">
                <h2 style={{ fontSize: '1.25rem', fontWeight: 600, marginBottom: '1.25rem' }}>
                  Solicitações de Entrada Pendentes
                </h2>

                {joinRequests.length === 0 ? (
                  <p style={{ color: 'var(--text-muted)', fontSize: '0.9rem' }}>Nenhuma solicitação pendente no momento.</p>
                ) : (
                  <div style={{ display: 'flex', flexDirection: 'column', gap: '0.75rem' }}>
                    {joinRequests.map((req) => (
                      <div
                        key={req.id}
                        style={{
                          display: 'flex',
                          alignItems: 'center',
                          justifyContent: 'space-between',
                          padding: '0.85rem 1.25rem',
                          backgroundColor: '#fffbeb',
                          borderRadius: '8px',
                          border: '1px solid #fde68a',
                        }}
                      >
                        <div>
                          <p style={{ fontWeight: 600 }}>Usuário: {req.userId}</p>
                          <p style={{ fontSize: '0.8rem', color: '#92400e' }}>
                            Solicitado em: {new Date(req.createdAt).toLocaleString()}
                          </p>
                        </div>

                        <div style={{ display: 'flex', gap: '0.5rem' }}>
                          <button
                            onClick={() => handleApproveRequest(req.id)}
                            disabled={actionLoading}
                            className="btn btn-primary"
                            style={{ width: 'auto', padding: '0.4rem 0.8rem', fontSize: '0.85rem', gap: '0.3rem', backgroundColor: '#059669' }}
                          >
                            <Check size={16} />
                            <span>Aprovar</span>
                          </button>

                          <button
                            onClick={() => handleRejectRequest(req.id)}
                            disabled={actionLoading}
                            className="btn btn-secondary"
                            style={{ width: 'auto', padding: '0.4rem 0.8rem', fontSize: '0.85rem', gap: '0.3rem', color: '#dc2626' }}
                          >
                            <X size={16} />
                            <span>Rejeitar</span>
                          </button>
                        </div>
                      </div>
                    ))}
                  </div>
                )}
              </div>
            )}

            {activeTab === 'members' && (
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
                          flexWrap: 'wrap',
                          gap: '0.75rem',
                        }}
                      >
                        <div>
                          <p style={{ fontWeight: 600 }}>{member.name}</p>
                          <p style={{ fontSize: '0.85rem', color: 'var(--text-muted)' }}>@{member.username}</p>
                        </div>

                        <div style={{ display: 'flex', alignItems: 'center', gap: '0.75rem' }}>
                          <span
                            className="user-badge"
                            style={{
                              backgroundColor:
                                member.role === 'OWNER' ? '#fef3c7' : member.role === 'ADMIN' ? '#e0e7ff' : '#f1f5f9',
                              color:
                                member.role === 'OWNER' ? '#92400e' : member.role === 'ADMIN' ? '#3730a3' : '#475569',
                              display: 'inline-flex',
                              alignItems: 'center',
                              gap: '0.3rem',
                            }}
                          >
                            {member.role === 'OWNER' && <ShieldCheck size={14} />}
                            {member.role}
                          </span>

                          {canManageMember(member) && (
                            <div style={{ display: 'flex', alignItems: 'center', gap: '0.5rem' }}>
                              <select
                                className="form-control"
                                style={{ padding: '0.3rem 0.5rem', fontSize: '0.85rem', width: 'auto' }}
                                value={member.role}
                                onChange={(e) => handleUpdateRole(member.userId, e.target.value as CommunityRole)}
                                disabled={actionLoading}
                              >
                                <option value="MEMBER">MEMBER</option>
                                <option value="MODERATOR">MODERATOR</option>
                                {isOwner && <option value="ADMIN">ADMIN</option>}
                                {isOwner && <option value="OWNER">Transferir OWNER</option>}
                              </select>

                              <button
                                onClick={() => handleRemoveMember(member.userId, member.name)}
                                disabled={actionLoading}
                                title="Remover membro"
                                className="btn btn-secondary"
                                style={{ padding: '0.35rem 0.5rem', width: 'auto', color: '#dc2626' }}
                              >
                                <UserMinus size={16} />
                              </button>
                            </div>
                          )}
                        </div>
                      </div>
                    ))}
                  </div>
                )}
              </div>
            )}
          </div>
        ) : null}
      </main>
    </div>
  );
};
