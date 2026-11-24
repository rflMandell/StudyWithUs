import React, { useCallback, useEffect, useState } from 'react';
import { Search, Eye, ShieldAlert, RotateCcw, KeyRound, UserCog, X } from 'lucide-react';
import { AdminLayout } from '../components/AdminLayout';
import { AlertError } from '../components/AlertError';
import { useAuth } from '../hooks/useAuth';
import { GlobalRole } from '../types/auth';
import {
  adminService,
  AdminUser360,
  AdminUserFilters,
  AdminUserSummary,
} from '../services/adminService';

export const AdminUsersPage: React.FC = () => {
  const { user: currentUser } = useAuth();
  const [users, setUsers] = useState<AdminUserSummary[]>([]);
  const [page, setPage] = useState(0);
  const [totalPages, setTotalPages] = useState(0);
  const [totalElements, setTotalElements] = useState(0);
  const [hasNext, setHasNext] = useState(false);

  const [search, setSearch] = useState('');
  const [statusFilter, setStatusFilter] = useState('');
  const [roleFilter, setRoleFilter] = useState('');
  const [planFilter, setPlanFilter] = useState('');

  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [feedback, setFeedback] = useState<string | null>(null);

  const [selectedUser, setSelectedUser] = useState<AdminUser360 | null>(null);
  const [detailLoading, setDetailLoading] = useState(false);
  const [actionReason, setActionReason] = useState('');
  const [newRole, setNewRole] = useState<GlobalRole>('USER');
  const [actionBusy, setActionBusy] = useState(false);

  const canSuspendOrReactivate =
    currentUser?.globalRole === 'ADMIN' || currentUser?.globalRole === 'SUPER_ADMIN';
  const canChangeRole = currentUser?.globalRole === 'SUPER_ADMIN';

  const loadUsers = useCallback(async (targetPage = page) => {
    setLoading(true);
    setError(null);
    try {
      const filters: AdminUserFilters = {
        ...(search.trim() ? { search: search.trim() } : {}),
        ...(statusFilter ? { status: statusFilter } : {}),
        ...(roleFilter ? { globalRole: roleFilter } : {}),
        ...(planFilter ? { plan: planFilter } : {}),
      };
      const res = await adminService.listUsers(targetPage, 15, filters);
      setUsers(res.content);
      setPage(res.page);
      setTotalPages(res.totalPages);
      setTotalElements(res.totalElements);
      setHasNext(res.hasNext);
    } catch {
      setError('Não foi possível carregar a lista de contas.');
    } finally {
      setLoading(false);
    }
  }, [page, search, statusFilter, roleFilter, planFilter]);

  useEffect(() => {
    loadUsers(0);
  }, [statusFilter, roleFilter, planFilter]);

  const handleSearchSubmit = (e: React.FormEvent) => {
    e.preventDefault();
    loadUsers(0);
  };

  const openDetails = async (userId: string) => {
    setDetailLoading(true);
    setActionReason('');
    setError(null);
    setFeedback(null);
    try {
      const detail = await adminService.getUser360(userId);
      setSelectedUser(detail);
      setNewRole(detail.globalRole);
    } catch {
      setError('Não foi possível carregar os detalhes 360° da conta.');
    } finally {
      setDetailLoading(false);
    }
  };

  const executeAction = async (fn: () => Promise<void>, successMessage: string) => {
    if (!actionReason.trim()) {
      setError('Informe uma justificativa (motivo) obrigatória para executar esta ação.');
      return;
    }
    setActionBusy(true);
    setError(null);
    setFeedback(null);
    try {
      await fn();
      setFeedback(successMessage);
      setActionReason('');
      if (selectedUser) {
        const refreshed = await adminService.getUser360(selectedUser.id);
        setSelectedUser(refreshed);
      }
      await loadUsers(page);
    } catch {
      setError('Falha ao executar ação administrativa. Verifique permissões e tente novamente.');
    } finally {
      setActionBusy(false);
    }
  };

  return (
    <AdminLayout>
      <div style={{ display: 'flex', flexDirection: 'column', gap: '1.25rem' }}>
        <div>
          <h1 style={{ fontSize: '1.5rem', marginBottom: '0.25rem' }}>Gestão de Contas ({totalElements})</h1>
          <p style={{ color: 'var(--text-muted)', fontSize: '0.925rem' }}>
            Busque, filtre, inspecione a visão 360° e execute ações administrativas auditadas.
          </p>
        </div>

        <AlertError message={error} />

        {feedback && (
          <div className="card" style={{ padding: '0.85rem 1rem', borderLeft: '4px solid #16a34a' }}>
            {feedback}
          </div>
        )}

        <div className="card" style={{ padding: '1rem' }}>
          <form onSubmit={handleSearchSubmit} style={{ display: 'flex', gap: '0.75rem', flexWrap: 'wrap', alignItems: 'center' }}>
            <div style={{ position: 'relative', flex: '1 1 240px' }}>
              <Search size={16} style={{ position: 'absolute', left: '0.75rem', top: '50%', transform: 'translateY(-50%)', color: 'var(--text-muted)' }} />
              <input
                type="text"
                className="form-control"
                placeholder="Buscar por e-mail, @username ou nome..."
                value={search}
                onChange={(e) => setSearch(e.target.value)}
                style={{ paddingLeft: '2.25rem' }}
              />
            </div>

            <select className="form-control" value={statusFilter} onChange={(e) => setStatusFilter(e.target.value)} style={{ width: 'auto' }} aria-label="Filtrar por status">
              <option value="">Status: Todos</option>
              <option value="ACTIVE">ACTIVE</option>
              <option value="SUSPENDED">SUSPENDED</option>
              <option value="DELETED">DELETED</option>
            </select>

            <select className="form-control" value={roleFilter} onChange={(e) => setRoleFilter(e.target.value)} style={{ width: 'auto' }} aria-label="Filtrar por papel">
              <option value="">Papel: Todos</option>
              <option value="USER">USER</option>
              <option value="SUPPORT">SUPPORT</option>
              <option value="ADMIN">ADMIN</option>
              <option value="SUPER_ADMIN">SUPER_ADMIN</option>
            </select>

            <select className="form-control" value={planFilter} onChange={(e) => setPlanFilter(e.target.value)} style={{ width: 'auto' }} aria-label="Filtrar por plano">
              <option value="">Plano: Todos</option>
              <option value="FREE">FREE</option>
              <option value="PREMIUM">PREMIUM</option>
            </select>

            <button type="submit" className="btn btn-primary" style={{ width: 'auto' }}>
              Buscar
            </button>
          </form>
        </div>

        <div className="card" style={{ padding: '1rem', overflowX: 'auto' }}>
          {loading ? (
            <p style={{ color: 'var(--text-muted)' }}>Carregando contas...</p>
          ) : users.length === 0 ? (
            <p style={{ color: 'var(--text-muted)' }}>Nenhuma conta encontrada para os filtros aplicados.</p>
          ) : (
            <>
              <table style={{ width: '100%', borderCollapse: 'collapse', fontSize: '0.875rem' }}>
                <thead>
                  <tr style={{ textAlign: 'left', borderBottom: '1px solid var(--border)', color: 'var(--text-muted)' }}>
                    <th style={{ padding: '0.6rem' }}>Conta</th>
                    <th style={{ padding: '0.6rem' }}>E-mail</th>
                    <th style={{ padding: '0.6rem' }}>Papel</th>
                    <th style={{ padding: '0.6rem' }}>Plano</th>
                    <th style={{ padding: '0.6rem' }}>Status</th>
                    <th style={{ padding: '0.6rem', textAlign: 'right' }}>Ações</th>
                  </tr>
                </thead>
                <tbody>
                  {users.map((u) => (
                    <tr key={u.id} style={{ borderBottom: '1px solid var(--border)' }}>
                      <td style={{ padding: '0.6rem', fontWeight: 500 }}>
                        {u.name} <span style={{ color: 'var(--text-muted)' }}>@{u.username}</span>
                      </td>
                      <td style={{ padding: '0.6rem', color: 'var(--text-muted)' }}>{u.email}</td>
                      <td style={{ padding: '0.6rem' }}>{u.globalRole}</td>
                      <td style={{ padding: '0.6rem' }}>{u.plan || 'FREE'}</td>
                      <td style={{ padding: '0.6rem' }}>
                        <span style={{ fontWeight: 600, color: u.status === 'ACTIVE' ? '#16a34a' : '#dc2626' }}>
                          {u.status}
                        </span>
                      </td>
                      <td style={{ padding: '0.6rem', textAlign: 'right' }}>
                        <button
                          onClick={() => openDetails(u.id)}
                          className="btn btn-secondary"
                          style={{ width: 'auto', padding: '0.35rem 0.65rem', fontSize: '0.8rem', gap: '0.35rem' }}
                        >
                          <Eye size={14} />
                          <span>Ver 360°</span>
                        </button>
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>

              <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginTop: '1rem' }}>
                <span style={{ fontSize: '0.85rem', color: 'var(--text-muted)' }}>
                  Página {page + 1} de {Math.max(totalPages, 1)}
                </span>
                <div style={{ display: 'flex', gap: '0.5rem' }}>
                  <button
                    disabled={page === 0}
                    onClick={() => loadUsers(page - 1)}
                    className="btn btn-secondary"
                    style={{ width: 'auto', padding: '0.35rem 0.75rem' }}
                  >
                    Anterior
                  </button>
                  <button
                    disabled={!hasNext}
                    onClick={() => loadUsers(page + 1)}
                    className="btn btn-secondary"
                    style={{ width: 'auto', padding: '0.35rem 0.75rem' }}
                  >
                    Próxima
                  </button>
                </div>
              </div>
            </>
          )}
        </div>

        {(detailLoading || selectedUser) && (
          <div
            style={{
              position: 'fixed',
              inset: 0,
              background: 'rgba(0,0,0,0.5)',
              display: 'flex',
              alignItems: 'center',
              justifyContent: 'center',
              padding: '1rem',
              zIndex: 50,
            }}
          >
            <div className="card" style={{ maxWidth: '760px', width: '100%', maxHeight: '90vh', overflowY: 'auto', padding: '1.5rem' }}>
              <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '1rem' }}>
                <h2 style={{ margin: 0, fontSize: '1.25rem' }}>Visão 360° da Conta</h2>
                <button onClick={() => setSelectedUser(null)} className="btn btn-secondary" style={{ width: 'auto', padding: '0.35rem' }} aria-label="Fechar modal">
                  <X size={18} />
                </button>
              </div>

              {detailLoading || !selectedUser ? (
                <p style={{ color: 'var(--text-muted)' }}>Carregando perfil 360°...</p>
              ) : (
                <div style={{ display: 'flex', flexDirection: 'column', gap: '1.25rem' }}>
                  <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(170px, 1fr))', gap: '0.75rem' }}>
                    <div><strong>Nome:</strong> {selectedUser.name}</div>
                    <div><strong>Username:</strong> @{selectedUser.username}</div>
                    <div><strong>E-mail:</strong> {selectedUser.email}</div>
                    <div><strong>Status:</strong> {selectedUser.status}</div>
                    <div><strong>Papel Global:</strong> {selectedUser.globalRole}</div>
                    <div><strong>Plano:</strong> {selectedUser.plan} ({selectedUser.subscriptionStatus})</div>
                    <div><strong>Pontos:</strong> {selectedUser.lifetimePoints}</div>
                    <div><strong>Streak:</strong> {selectedUser.currentStreak} (máx: {selectedUser.longestStreak})</div>
                    <div><strong>Nível / XP:</strong> Nível {selectedUser.level} ({selectedUser.totalXp} XP)</div>
                    <div><strong>Liga:</strong> {selectedUser.leagueDivision}</div>
                  </div>

                  <div>
                    <h3 style={{ fontSize: '0.95rem', marginBottom: '0.4rem' }}>Comunidades ({selectedUser.communities.length})</h3>
                    {selectedUser.communities.length === 0 ? (
                      <p style={{ fontSize: '0.85rem', color: 'var(--text-muted)', margin: 0 }}>Sem participação em comunidades.</p>
                    ) : (
                      <ul style={{ margin: 0, paddingLeft: '1.25rem', fontSize: '0.85rem' }}>
                        {selectedUser.communities.map((c) => (
                          <li key={c.communityId}>{c.name} — {c.role} ({c.status})</li>
                        ))}
                      </ul>
                    )}
                  </div>

                  <div>
                    <h3 style={{ fontSize: '0.95rem', marginBottom: '0.4rem' }}>Histórico de Denúncias ({selectedUser.reports.length})</h3>
                    {selectedUser.reports.length === 0 ? (
                      <p style={{ fontSize: '0.85rem', color: 'var(--text-muted)', margin: 0 }}>Nenhuma denúncia registrada.</p>
                    ) : (
                      <ul style={{ margin: 0, paddingLeft: '1.25rem', fontSize: '0.85rem' }}>
                        {selectedUser.reports.map((r) => (
                          <li key={r.id}>[{r.status}] {r.targetType} — {r.reason}</li>
                        ))}
                      </ul>
                    )}
                  </div>

                  <div style={{ borderTop: '1px solid var(--border)', paddingTop: '1rem', display: 'flex', flexDirection: 'column', gap: '0.75rem' }}>
                    <label htmlFor="actionReason" style={{ fontWeight: 600, fontSize: '0.9rem' }}>
                      Justificativa obrigatória para ações administrativas
                    </label>
                    <textarea
                      id="actionReason"
                      className="form-control"
                      rows={2}
                      placeholder="Descreva o motivo desta ação..."
                      value={actionReason}
                      onChange={(e) => setActionReason(e.target.value)}
                      disabled={actionBusy}
                    />

                    <div style={{ display: 'flex', gap: '0.5rem', flexWrap: 'wrap' }}>
                      {canSuspendOrReactivate && selectedUser.status === 'ACTIVE' && (
                        <button
                          disabled={actionBusy}
                          onClick={() => executeAction(
                            async () => { await adminService.suspendUser(selectedUser.id, actionReason); },
                            'Conta suspensa e sessões revogadas imediatamente.'
                          )}
                          className="btn btn-primary"
                          style={{ width: 'auto', background: '#dc2626', gap: '0.4rem' }}
                        >
                          <ShieldAlert size={15} />
                          <span>Suspender Conta</span>
                        </button>
                      )}

                      {canSuspendOrReactivate && selectedUser.status !== 'ACTIVE' && (
                        <button
                          disabled={actionBusy}
                          onClick={() => executeAction(
                            async () => { await adminService.reactivateUser(selectedUser.id, actionReason); },
                            'Conta reativada com sucesso.'
                          )}
                          className="btn btn-primary"
                          style={{ width: 'auto', gap: '0.4rem' }}
                        >
                          <RotateCcw size={15} />
                          <span>Reativar Conta</span>
                        </button>
                      )}

                      <button
                        disabled={actionBusy}
                        onClick={() => executeAction(
                          async () => { await adminService.revokeSessions(selectedUser.id, actionReason); },
                          'Todas as sessões (refresh tokens) foram revogadas.'
                        )}
                        className="btn btn-secondary"
                        style={{ width: 'auto', gap: '0.4rem' }}
                      >
                        <KeyRound size={15} />
                        <span>Revogar Sessões</span>
                      </button>

                      {canChangeRole && (
                        <div style={{ display: 'flex', gap: '0.4rem', alignItems: 'center' }}>
                          <select
                            className="form-control"
                            value={newRole}
                            onChange={(e) => setNewRole(e.target.value as GlobalRole)}
                            style={{ width: 'auto' }}
                            aria-label="Novo papel global"
                          >
                            <option value="USER">USER</option>
                            <option value="SUPPORT">SUPPORT</option>
                            <option value="ADMIN">ADMIN</option>
                            <option value="SUPER_ADMIN">SUPER_ADMIN</option>
                          </select>
                          <button
                            disabled={actionBusy}
                            onClick={() => executeAction(
                              async () => { await adminService.changeRole(selectedUser.id, newRole, actionReason); },
                              `Papel global atualizado para ${newRole}.`
                            )}
                            className="btn btn-secondary"
                            style={{ width: 'auto', gap: '0.4rem' }}
                          >
                            <UserCog size={15} />
                            <span>Alterar Papel</span>
                          </button>
                        </div>
                      )}
                    </div>
                  </div>
                </div>
              )}
            </div>
          </div>
        )}
      </div>
    </AdminLayout>
  );
};
