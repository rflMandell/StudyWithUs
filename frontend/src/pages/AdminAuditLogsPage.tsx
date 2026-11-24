import React, { useCallback, useEffect, useState } from 'react';
import { ClipboardList, Download, GitCompare, X } from 'lucide-react';
import { AdminLayout } from '../components/AdminLayout';
import { AlertError } from '../components/AlertError';
import { useAuth } from '../hooks/useAuth';
import { adminService, AdminAuditLogItem } from '../services/adminService';

const parseJsonSafe = (raw?: string): Record<string, unknown> => {
  if (!raw) return {};
  try {
    const parsed = JSON.parse(raw);
    return typeof parsed === 'object' && parsed !== null ? parsed : { value: parsed };
  } catch {
    return { raw };
  }
};

export const AdminAuditLogsPage: React.FC = () => {
  const { user } = useAuth();
  const [logs, setLogs] = useState<AdminAuditLogItem[]>([]);
  const [page, setPage] = useState(0);
  const [totalPages, setTotalPages] = useState(0);
  const [totalElements, setTotalElements] = useState(0);
  const [hasNext, setHasNext] = useState(false);

  const [actorIdFilter, setActorIdFilter] = useState('');
  const [actionFilter, setActionFilter] = useState('');
  const [targetTypeFilter, setTargetTypeFilter] = useState('');

  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [selectedLog, setSelectedLog] = useState<AdminAuditLogItem | null>(null);

  const canExport = user?.globalRole === 'ADMIN' || user?.globalRole === 'SUPER_ADMIN';

  const fetchAuditLogs = useCallback(async (targetPage = page) => {
    setLoading(true);
    setError(null);
    try {
      const res = await adminService.listAuditLogs(targetPage, 20, {
        actorId: actorIdFilter || undefined,
        action: actionFilter || undefined,
        targetType: targetTypeFilter || undefined,
      });
      setLogs(res.content);
      setPage(res.page);
      setTotalPages(res.totalPages);
      setTotalElements(res.totalElements);
      setHasNext(res.hasNext);
    } catch {
      setError('Não foi possível carregar os registros de auditoria.');
    } finally {
      setLoading(false);
    }
  }, [page, actorIdFilter, actionFilter, targetTypeFilter]);

  useEffect(() => {
    fetchAuditLogs(0);
  }, []);

  const handleFilter = (e: React.FormEvent) => {
    e.preventDefault();
    fetchAuditLogs(0);
  };

  const handleExport = async (format: 'csv' | 'json') => {
    setError(null);
    try {
      const blob = await adminService.exportAuditLogs(format, {
        actorId: actorIdFilter || undefined,
        action: actionFilter || undefined,
        targetType: targetTypeFilter || undefined,
      });
      const url = URL.createObjectURL(blob);
      const a = document.createElement('a');
      a.href = url;
      a.download = `audit-logs.${format}`;
      a.click();
      URL.revokeObjectURL(url);
    } catch {
      setError('Não foi possível exportar os logs de auditoria.');
    }
  };

  const renderDiffRows = (log: AdminAuditLogItem) => {
    const before = parseJsonSafe(log.beforeState);
    const after = parseJsonSafe(log.afterState);
    const keys = Array.from(new Set([...Object.keys(before), ...Object.keys(after)]));
    if (keys.length === 0) {
      return <p style={{ color: 'var(--text-muted)', margin: 0 }}>Sem snapshot de estado antes/depois para esta ação.</p>;
    }

    return (
      <table style={{ width: '100%', borderCollapse: 'collapse', fontSize: '0.84rem' }}>
        <thead>
          <tr style={{ textAlign: 'left', borderBottom: '1px solid var(--border)', color: 'var(--text-muted)' }}>
            <th style={{ padding: '0.45rem' }}>Campo</th>
            <th style={{ padding: '0.45rem' }}>Antes (before_state)</th>
            <th style={{ padding: '0.45rem' }}>Depois (after_state)</th>
          </tr>
        </thead>
        <tbody>
          {keys.map((k) => {
            const bVal = before[k] !== undefined ? JSON.stringify(before[k]) : '—';
            const aVal = after[k] !== undefined ? JSON.stringify(after[k]) : '—';
            const changed = bVal !== aVal;
            return (
              <tr key={k} style={{ borderBottom: '1px solid var(--border)' }}>
                <td style={{ padding: '0.45rem', fontWeight: 600 }}>{k}</td>
                <td style={{ padding: '0.45rem', background: changed ? 'rgba(220,38,38,0.08)' : 'transparent', fontFamily: 'monospace' }}>
                  {bVal}
                </td>
                <td style={{ padding: '0.45rem', background: changed ? 'rgba(22,163,74,0.08)' : 'transparent', fontFamily: 'monospace' }}>
                  {aVal}
                </td>
              </tr>
            );
          })}
        </tbody>
      </table>
    );
  };

  return (
    <AdminLayout>
      <div style={{ display: 'flex', flexDirection: 'column', gap: '1.25rem' }}>
        <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', flexWrap: 'wrap', gap: '0.75rem' }}>
          <div>
            <h1 style={{ fontSize: '1.5rem', marginBottom: '0.25rem', display: 'flex', alignItems: 'center', gap: '0.5rem' }}>
              <ClipboardList size={22} color="var(--primary)" />
              <span>Auditoria de Admins ({totalElements})</span>
            </h1>
            <p style={{ color: 'var(--text-muted)', fontSize: '0.925rem', margin: 0 }}>
              Trilha imutável (append-only) de todas as ações administrativas com diff antes/depois.
            </p>
          </div>

          {canExport && (
            <div style={{ display: 'flex', gap: '0.5rem' }}>
              <button onClick={() => handleExport('csv')} className="btn btn-secondary" style={{ width: 'auto', gap: '0.35rem' }}>
                <Download size={15} />
                <span>Exportar CSV</span>
              </button>
              <button onClick={() => handleExport('json')} className="btn btn-secondary" style={{ width: 'auto', gap: '0.35rem' }}>
                <Download size={15} />
                <span>Exportar JSON</span>
              </button>
            </div>
          )}
        </div>

        <AlertError message={error} />

        <div className="card" style={{ padding: '1rem' }}>
          <form onSubmit={handleFilter} style={{ display: 'flex', gap: '0.75rem', flexWrap: 'wrap', alignItems: 'center' }}>
            <input
              type="text"
              className="form-control"
              placeholder="Actor ID (UUID)"
              value={actorIdFilter}
              onChange={(e) => setActorIdFilter(e.target.value)}
              style={{ flex: '1 1 200px' }}
            />
            <input
              type="text"
              className="form-control"
              placeholder="Ação (ex: USER_SUSPENDED)"
              value={actionFilter}
              onChange={(e) => setActionFilter(e.target.value)}
              style={{ flex: '1 1 180px' }}
            />
            <input
              type="text"
              className="form-control"
              placeholder="Target Type (ex: USER)"
              value={targetTypeFilter}
              onChange={(e) => setTargetTypeFilter(e.target.value)}
              style={{ width: '170px' }}
            />
            <button type="submit" className="btn btn-primary" style={{ width: 'auto' }}>
              Filtrar
            </button>
          </form>
        </div>

        <div className="card" style={{ padding: '1rem', overflowX: 'auto' }}>
          {loading ? (
            <p style={{ color: 'var(--text-muted)' }}>Carregando auditoria...</p>
          ) : logs.length === 0 ? (
            <p style={{ color: 'var(--text-muted)' }}>Nenhum registro de auditoria encontrado.</p>
          ) : (
            <>
              <table style={{ width: '100%', borderCollapse: 'collapse', fontSize: '0.84rem' }}>
                <thead>
                  <tr style={{ textAlign: 'left', borderBottom: '1px solid var(--border)', color: 'var(--text-muted)' }}>
                    <th style={{ padding: '0.5rem' }}>Data/Hora</th>
                    <th style={{ padding: '0.5rem' }}>Ator</th>
                    <th style={{ padding: '0.5rem' }}>Ação</th>
                    <th style={{ padding: '0.5rem' }}>Alvo</th>
                    <th style={{ padding: '0.5rem' }}>Motivo</th>
                    <th style={{ padding: '0.5rem' }}>IP</th>
                    <th style={{ padding: '0.5rem', textAlign: 'right' }}>Diff</th>
                  </tr>
                </thead>
                <tbody>
                  {logs.map((entry) => (
                    <tr key={entry.id} style={{ borderBottom: '1px solid var(--border)' }}>
                      <td style={{ padding: '0.5rem', whiteSpace: 'nowrap', color: 'var(--text-muted)' }}>
                        {new Date(entry.createdAt).toLocaleString('pt-BR')}
                      </td>
                      <td style={{ padding: '0.5rem' }}>
                        <strong>{entry.actorRole || '-'}</strong>
                        <div style={{ fontSize: '0.75rem', color: 'var(--text-muted)', fontFamily: 'monospace' }}>
                          {entry.actorId || '-'}
                        </div>
                      </td>
                      <td style={{ padding: '0.5rem', fontWeight: 600 }}>{entry.action}</td>
                      <td style={{ padding: '0.5rem' }}>
                        {entry.targetType}
                        {entry.targetId && (
                          <div style={{ fontSize: '0.75rem', color: 'var(--text-muted)', fontFamily: 'monospace' }}>
                            {entry.targetId}
                          </div>
                        )}
                      </td>
                      <td style={{ padding: '0.5rem' }}>{entry.reason || '—'}</td>
                      <td style={{ padding: '0.5rem', fontFamily: 'monospace', fontSize: '0.78rem' }}>{entry.ipAddress || '—'}</td>
                      <td style={{ padding: '0.5rem', textAlign: 'right' }}>
                        <button
                          onClick={() => setSelectedLog(entry)}
                          className="btn btn-secondary"
                          style={{ width: 'auto', padding: '0.3rem 0.6rem', fontSize: '0.78rem', gap: '0.3rem' }}
                        >
                          <GitCompare size={13} />
                          <span>Ver Diff</span>
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
                  <button disabled={page === 0} onClick={() => fetchAuditLogs(page - 1)} className="btn btn-secondary" style={{ width: 'auto' }}>
                    Anterior
                  </button>
                  <button disabled={!hasNext} onClick={() => fetchAuditLogs(page + 1)} className="btn btn-secondary" style={{ width: 'auto' }}>
                    Próxima
                  </button>
                </div>
              </div>
            </>
          )}
        </div>

        {selectedLog && (
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
            <div className="card" style={{ maxWidth: '720px', width: '100%', maxHeight: '85vh', overflowY: 'auto', padding: '1.5rem' }}>
              <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '1rem' }}>
                <h2 style={{ margin: 0, fontSize: '1.15rem' }}>Diff de Auditoria: {selectedLog.action}</h2>
                <button onClick={() => setSelectedLog(null)} className="btn btn-secondary" style={{ width: 'auto', padding: '0.35rem' }} aria-label="Fechar diff">
                  <X size={18} />
                </button>
              </div>
              <div style={{ fontSize: '0.85rem', marginBottom: '1rem', color: 'var(--text-muted)' }}>
                <div><strong>Motivo:</strong> {selectedLog.reason || '—'}</div>
                <div><strong>Request ID:</strong> {selectedLog.requestId || '—'} | <strong>IP:</strong> {selectedLog.ipAddress || '—'}</div>
              </div>
              {renderDiffRows(selectedLog)}
            </div>
          </div>
        )}
      </div>
    </AdminLayout>
  );
};
