import React, { useCallback, useEffect, useState } from 'react';
import { FileText, Search, ChevronDown, ChevronRight, GitBranch, X } from 'lucide-react';
import { AdminLayout } from '../components/AdminLayout';
import { AlertError } from '../components/AlertError';
import { adminService, SystemLogItem } from '../services/adminService';

const levelStyle = (level: string): React.CSSProperties => {
  switch (level.toUpperCase()) {
    case 'ERROR':
      return { background: 'rgba(220,38,38,0.12)', color: '#dc2626', fontWeight: 700, padding: '0.15rem 0.45rem', borderRadius: '4px' };
    case 'WARN':
      return { background: 'rgba(217,119,6,0.14)', color: '#d97706', fontWeight: 700, padding: '0.15rem 0.45rem', borderRadius: '4px' };
    default:
      return { background: 'rgba(37,99,235,0.12)', color: '#2563eb', fontWeight: 600, padding: '0.15rem 0.45rem', borderRadius: '4px' };
  }
};

export const AdminSystemLogsPage: React.FC = () => {
  const [logs, setLogs] = useState<SystemLogItem[]>([]);
  const [nextCursor, setNextCursor] = useState<string | undefined>(undefined);
  const [hasNext, setHasNext] = useState(false);

  const [levelFilter, setLevelFilter] = useState('');
  const [moduleFilter, setModuleFilter] = useState('');
  const [traceIdFilter, setTraceIdFilter] = useState('');
  const [statusFilter, setStatusFilter] = useState('');

  const [expandedIds, setExpandedIds] = useState<Record<string, boolean>>({});
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  const [selectedTraceId, setSelectedTraceId] = useState<string | null>(null);
  const [traceEvents, setTraceEvents] = useState<SystemLogItem[]>([]);
  const [traceLoading, setTraceLoading] = useState(false);

  const fetchLogs = useCallback(async (append = false, cursor?: string) => {
    setLoading(true);
    setError(null);
    try {
      const res = await adminService.listSystemLogs({
        level: levelFilter || undefined,
        module: moduleFilter || undefined,
        traceId: traceIdFilter || undefined,
        statusCode: statusFilter ? Number(statusFilter) : undefined,
        cursor,
        limit: 30,
      });
      setLogs((prev) => (append ? [...prev, ...res.items] : res.items));
      setNextCursor(res.nextCursor);
      setHasNext(res.hasNext);
    } catch {
      setError('Não foi possível carregar os logs do sistema.');
    } finally {
      setLoading(false);
    }
  }, [levelFilter, moduleFilter, traceIdFilter, statusFilter]);

  useEffect(() => {
    fetchLogs(false);
  }, [levelFilter]);

  const handleFilterSubmit = (e: React.FormEvent) => {
    e.preventDefault();
    fetchLogs(false);
  };

  const toggleExpand = (id: string) => {
    setExpandedIds((prev) => ({ ...prev, [id]: !prev[id] }));
  };

  const openTraceTimeline = async (traceId: string) => {
    setSelectedTraceId(traceId);
    setTraceLoading(true);
    setError(null);
    try {
      const events = await adminService.getTraceTimeline(traceId);
      setTraceEvents(events);
    } catch {
      setError('Não foi possível reconstruir a linha do tempo do traceId.');
    } finally {
      setTraceLoading(false);
    }
  };

  return (
    <AdminLayout>
      <div style={{ display: 'flex', flexDirection: 'column', gap: '1.25rem' }}>
        <div>
          <h1 style={{ fontSize: '1.5rem', marginBottom: '0.25rem', display: 'flex', alignItems: 'center', gap: '0.5rem' }}>
            <FileText size={22} color="var(--primary)" />
            <span>Logs do Sistema Principal</span>
          </h1>
          <p style={{ color: 'var(--text-muted)', fontSize: '0.925rem' }}>
            Consulta centralizada com correlação por traceId, severidade e mascaramento automático de segredos.
          </p>
        </div>

        <AlertError message={error} />

        <div className="card" style={{ padding: '1rem' }}>
          <form onSubmit={handleFilterSubmit} style={{ display: 'flex', gap: '0.75rem', flexWrap: 'wrap', alignItems: 'center' }}>
            <select
              className="form-control"
              value={levelFilter}
              onChange={(e) => setLevelFilter(e.target.value)}
              style={{ width: 'auto' }}
              aria-label="Filtrar por severidade"
            >
              <option value="">Nível: Todos</option>
              <option value="INFO">INFO</option>
              <option value="WARN">WARN</option>
              <option value="ERROR">ERROR</option>
            </select>

            <input
              type="text"
              className="form-control"
              placeholder="Módulo (ex: identity, admin)"
              value={moduleFilter}
              onChange={(e) => setModuleFilter(e.target.value)}
              style={{ width: '190px' }}
            />

            <div style={{ position: 'relative', flex: '1 1 200px' }}>
              <Search size={15} style={{ position: 'absolute', left: '0.7rem', top: '50%', transform: 'translateY(-50%)', color: 'var(--text-muted)' }} />
              <input
                type="text"
                className="form-control"
                placeholder="Filtrar por traceId..."
                value={traceIdFilter}
                onChange={(e) => setTraceIdFilter(e.target.value)}
                style={{ paddingLeft: '2.1rem' }}
              />
            </div>

            <input
              type="number"
              className="form-control"
              placeholder="Status HTTP"
              value={statusFilter}
              onChange={(e) => setStatusFilter(e.target.value)}
              style={{ width: '130px' }}
            />

            <button type="submit" className="btn btn-primary" style={{ width: 'auto' }}>
              Filtrar
            </button>
          </form>
        </div>

        <div className="card" style={{ padding: '1rem', overflowX: 'auto' }}>
          {loading && logs.length === 0 ? (
            <p style={{ color: 'var(--text-muted)' }}>Carregando logs...</p>
          ) : logs.length === 0 ? (
            <p style={{ color: 'var(--text-muted)' }}>Nenhum log encontrado para os filtros selecionados.</p>
          ) : (
            <>
              <table style={{ width: '100%', borderCollapse: 'collapse', fontSize: '0.84rem' }}>
                <thead>
                  <tr style={{ textAlign: 'left', borderBottom: '1px solid var(--border)', color: 'var(--text-muted)' }}>
                    <th style={{ padding: '0.5rem' }}></th>
                    <th style={{ padding: '0.5rem' }}>Timestamp</th>
                    <th style={{ padding: '0.5rem' }}>Nível</th>
                    <th style={{ padding: '0.5rem' }}>Módulo</th>
                    <th style={{ padding: '0.5rem' }}>TraceId</th>
                    <th style={{ padding: '0.5rem' }}>Mensagem</th>
                  </tr>
                </thead>
                <tbody>
                  {logs.map((entry) => {
                    const isExpanded = !!expandedIds[entry.id];
                    return (
                      <React.Fragment key={entry.id}>
                        <tr style={{ borderBottom: '1px solid var(--border)' }}>
                          <td style={{ padding: '0.5rem' }}>
                            <button
                              type="button"
                              onClick={() => toggleExpand(entry.id)}
                              className="btn btn-secondary"
                              style={{ width: 'auto', padding: '0.2rem' }}
                              aria-label="Expandir contexto"
                            >
                              {isExpanded ? <ChevronDown size={14} /> : <ChevronRight size={14} />}
                            </button>
                          </td>
                          <td style={{ padding: '0.5rem', whiteSpace: 'nowrap', color: 'var(--text-muted)' }}>
                            {new Date(entry.timestamp).toLocaleString('pt-BR')}
                          </td>
                          <td style={{ padding: '0.5rem' }}>
                            <span style={levelStyle(entry.level)}>{entry.level}</span>
                          </td>
                          <td style={{ padding: '0.5rem' }}>{entry.module}</td>
                          <td style={{ padding: '0.5rem' }}>
                            <button
                              type="button"
                              onClick={() => openTraceTimeline(entry.traceId)}
                              style={{
                                background: 'none',
                                border: 'none',
                                color: 'var(--primary)',
                                cursor: 'pointer',
                                fontFamily: 'monospace',
                                display: 'inline-flex',
                                alignItems: 'center',
                                gap: '0.25rem',
                                padding: 0,
                              }}
                            >
                              <GitBranch size={13} />
                              <span>{entry.traceId}</span>
                            </button>
                          </td>
                          <td style={{ padding: '0.5rem', wordBreak: 'break-word' }}>{entry.message}</td>
                        </tr>
                        {isExpanded && (
                          <tr style={{ background: 'var(--bg)' }}>
                            <td colSpan={6} style={{ padding: '0.75rem 1rem' }}>
                              <div style={{ display: 'flex', gap: '1.5rem', flexWrap: 'wrap', fontSize: '0.8rem', marginBottom: '0.5rem' }}>
                                <span><strong>HTTP:</strong> {entry.httpMethod || '-'} {entry.path || '-'}</span>
                                <span><strong>Status:</strong> {entry.statusCode ?? '-'}</span>
                                <span><strong>Duração:</strong> {entry.durationMs != null ? `${entry.durationMs}ms` : '-'}</span>
                                <span><strong>UserId:</strong> {entry.userId || '-'}</span>
                                <span><strong>RequestId:</strong> {entry.requestId || '-'}</span>
                              </div>
                              {entry.context && (
                                <pre style={{ margin: 0, padding: '0.6rem', background: 'rgba(0,0,0,0.05)', borderRadius: '6px', fontSize: '0.78rem', overflowX: 'auto' }}>
                                  {entry.context}
                                </pre>
                              )}
                            </td>
                          </tr>
                        )}
                      </React.Fragment>
                    );
                  })}
                </tbody>
              </table>

              {hasNext && nextCursor && (
                <div style={{ marginTop: '1rem', textAlign: 'center' }}>
                  <button
                    onClick={() => fetchLogs(true, nextCursor)}
                    disabled={loading}
                    className="btn btn-secondary"
                    style={{ width: 'auto' }}
                  >
                    {loading ? 'Carregando...' : 'Carregar mais logs'}
                  </button>
                </div>
              )}
            </>
          )}
        </div>

        {selectedTraceId && (
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
            <div className="card" style={{ maxWidth: '760px', width: '100%', maxHeight: '85vh', overflowY: 'auto', padding: '1.5rem' }}>
              <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '1rem' }}>
                <h2 style={{ margin: 0, fontSize: '1.15rem' }}>
                  Timeline do Trace: <code>{selectedTraceId}</code>
                </h2>
                <button
                  onClick={() => setSelectedTraceId(null)}
                  className="btn btn-secondary"
                  style={{ width: 'auto', padding: '0.35rem' }}
                  aria-label="Fechar timeline"
                >
                  <X size={18} />
                </button>
              </div>

              {traceLoading ? (
                <p style={{ color: 'var(--text-muted)' }}>Reconstruindo eventos da requisição...</p>
              ) : traceEvents.length === 0 ? (
                <p style={{ color: 'var(--text-muted)' }}>Nenhum evento encontrado para este traceId.</p>
              ) : (
                <div style={{ display: 'flex', flexDirection: 'column', gap: '0.75rem' }}>
                  {traceEvents.map((ev) => (
                    <div key={ev.id} style={{ borderLeft: '3px solid var(--primary)', paddingLeft: '0.75rem' }}>
                      <div style={{ display: 'flex', gap: '0.5rem', alignItems: 'center', fontSize: '0.8rem' }}>
                        <span style={levelStyle(ev.level)}>{ev.level}</span>
                        <strong>{ev.module}</strong>
                        <span style={{ color: 'var(--text-muted)' }}>{new Date(ev.timestamp).toISOString()}</span>
                      </div>
                      <div style={{ marginTop: '0.25rem', fontSize: '0.875rem' }}>{ev.message}</div>
                      {ev.context && (
                        <pre style={{ marginTop: '0.35rem', padding: '0.5rem', background: 'var(--bg)', borderRadius: '4px', fontSize: '0.75rem', overflowX: 'auto' }}>
                          {ev.context}
                        </pre>
                      )}
                    </div>
                  ))}
                </div>
              )}
            </div>
          </div>
        )}
      </div>
    </AdminLayout>
  );
};
