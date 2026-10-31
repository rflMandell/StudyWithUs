import React, { useEffect, useState } from 'react';
import { Link } from 'react-router-dom';
import { Users, Plus, Search, Globe, Lock, Check } from 'lucide-react';
import { Community } from '../types/community';
import { communityService } from '../services/communityService';
import { Navbar } from '../components/Navbar';
import { CreateCommunityModal } from '../components/CreateCommunityModal';
import { AlertError } from '../components/AlertError';

export const CommunitiesPage: React.FC = () => {
  const [communities, setCommunities] = useState<Community[]>([]);
  const [search, setSearch] = useState('');
  const [isLoading, setIsLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [isModalOpen, setIsModalOpen] = useState(false);
  const [joiningId, setJoiningId] = useState<string | null>(null);

  const loadCommunities = async (searchQuery?: string) => {
    setIsLoading(true);
    setError(null);
    try {
      const data = await communityService.listCommunities(searchQuery);
      setCommunities(data.content);
    } catch (err: any) {
      setError('Não foi possível carregar as comunidades.');
    } finally {
      setIsLoading(false);
    }
  };

  useEffect(() => {
    loadCommunities();
  }, []);

  const handleSearchSubmit = (e: React.FormEvent) => {
    e.preventDefault();
    loadCommunities(search);
  };

  const handleJoin = async (communityId: string) => {
    setJoiningId(communityId);
    try {
      await communityService.joinCommunity(communityId);
      // Reload list to update member status and counter
      await loadCommunities(search);
    } catch (err: any) {
      const msg = err.response?.data?.message || err.response?.data?.code || 'Erro ao entrar na comunidade.';
      alert(msg);
    } finally {
      setJoiningId(null);
    }
  };

  const handleCommunityCreated = (newCommunity: Community) => {
    setIsModalOpen(false);
    setCommunities((prev) => [newCommunity, ...prev]);
  };

  return (
    <div>
      <Navbar />
      <main className="container">
        <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', flexWrap: 'wrap', gap: '1rem', marginBottom: '2rem' }}>
          <div>
            <h1 style={{ fontSize: '1.85rem', fontWeight: 700 }}>Comunidades Acadêmicas</h1>
            <p style={{ color: 'var(--text-muted)' }}>
              Estude junto, compartilhe check-ins e evolua em grupo.
            </p>
          </div>

          <button
            onClick={() => setIsModalOpen(true)}
            className="btn btn-primary"
            style={{ width: 'auto', gap: '0.4rem', padding: '0.65rem 1.25rem' }}
          >
            <Plus size={18} />
            <span>Criar Comunidade</span>
          </button>
        </div>

        <form onSubmit={handleSearchSubmit} style={{ display: 'flex', gap: '0.75rem', marginBottom: '1.75rem' }}>
          <div style={{ position: 'relative', flex: 1 }}>
            <Search size={18} style={{ position: 'absolute', left: '0.85rem', top: '50%', transform: 'translateY(-50%)', color: 'var(--text-muted)' }} />
            <input
              type="text"
              className="form-control"
              style={{ paddingLeft: '2.5rem' }}
              value={search}
              onChange={(e) => setSearch(e.target.value)}
              placeholder="Buscar por nome ou tema de estudo..."
            />
          </div>
          <button type="submit" className="btn btn-secondary" style={{ width: 'auto' }}>
            Buscar
          </button>
        </form>

        <AlertError message={error} />

        {isLoading ? (
          <p style={{ textAlign: 'center', color: 'var(--text-muted)', padding: '3rem 0' }}>Carregando comunidades...</p>
        ) : communities.length === 0 ? (
          <div className="card" style={{ textAlign: 'center', padding: '3rem 1.5rem' }}>
            <Users size={48} style={{ margin: '0 auto 1rem', color: 'var(--text-muted)' }} />
            <h3>Nenhuma comunidade encontrada</h3>
            <p style={{ color: 'var(--text-muted)', marginTop: '0.25rem' }}>
              Seja o primeiro a criar um grupo de estudos com seus colegas!
            </p>
            <button
              onClick={() => setIsModalOpen(true)}
              className="btn btn-primary"
              style={{ width: 'auto', margin: '1rem auto 0' }}
            >
              Criar primeira comunidade
            </button>
          </div>
        ) : (
          <div className="grid-cards">
            {communities.map((comm) => {
              const isMember = !!comm.currentUserRole;

              return (
                <div key={comm.id} className="card" style={{ display: 'flex', flexDirection: 'column', justifyContent: 'space-between' }}>
                  <div>
                    <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'flex-start', marginBottom: '0.75rem' }}>
                      <span
                        className="user-badge"
                        style={{
                          display: 'inline-flex',
                          alignItems: 'center',
                          gap: '0.3rem',
                          backgroundColor: comm.visibility === 'PUBLIC' ? '#ecfdf5' : '#fef3c7',
                          color: comm.visibility === 'PUBLIC' ? '#065f46' : '#92400e',
                        }}
                      >
                        {comm.visibility === 'PUBLIC' ? <Globe size={12} /> : <Lock size={12} />}
                        {comm.visibility === 'PUBLIC' ? 'Pública' : 'Privada'}
                      </span>

                      <span style={{ fontSize: '0.85rem', color: 'var(--text-muted)', display: 'flex', alignItems: 'center', gap: '0.3rem' }}>
                        <Users size={14} />
                        {comm.membersCount} {comm.membersCount === 1 ? 'membro' : 'membros'}
                      </span>
                    </div>

                    <h3 style={{ fontSize: '1.2rem', marginBottom: '0.4rem' }}>{comm.name}</h3>
                    <p style={{ color: 'var(--text-muted)', fontSize: '0.9rem', marginBottom: '1.25rem', lineHeight: '1.4' }}>
                      {comm.description || 'Sem descrição fornecida.'}
                    </p>
                  </div>

                  <div style={{ display: 'flex', gap: '0.6rem', marginTop: 'auto' }}>
                    <Link
                      to={`/communities/${comm.id}`}
                      className="btn btn-secondary"
                      style={{ flex: 1, padding: '0.55rem', fontSize: '0.875rem' }}
                    >
                      Detalhes
                    </Link>

                    {isMember ? (
                      <span
                        className="btn btn-secondary"
                        style={{
                          flex: 1,
                          padding: '0.55rem',
                          fontSize: '0.875rem',
                          cursor: 'default',
                          color: '#059669',
                          borderColor: '#a7f3d0',
                          backgroundColor: '#ecfdf5',
                          gap: '0.3rem',
                        }}
                      >
                        <Check size={14} />
                        {comm.currentUserRole === 'OWNER' ? 'Owner' : 'Membro'}
                      </span>
                    ) : comm.visibility === 'PUBLIC' ? (
                      <button
                        onClick={() => handleJoin(comm.id)}
                        disabled={joiningId === comm.id}
                        className="btn btn-primary"
                        style={{ flex: 1, padding: '0.55rem', fontSize: '0.875rem' }}
                      >
                        {joiningId === comm.id ? 'Entrando...' : 'Participar'}
                      </button>
                    ) : (
                      <span
                        className="btn btn-secondary"
                        style={{ flex: 1, padding: '0.55rem', fontSize: '0.875rem', color: 'var(--text-muted)' }}
                      >
                        Privada
                      </span>
                    )}
                  </div>
                </div>
              );
            })}
          </div>
        )}

        <CreateCommunityModal
          isOpen={isModalOpen}
          onClose={() => setIsModalOpen(false)}
          onSuccess={handleCommunityCreated}
        />
      </main>
    </div>
  );
};
