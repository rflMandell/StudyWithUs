import React, { useCallback, useEffect, useState } from 'react';
import { Activity, Database, HardDrive, Server, RefreshCw, ExternalLink, BarChart3 } from 'lucide-react';
import { AdminLayout } from '../components/AdminLayout';
import { AlertError } from '../components/AlertError';
import { adminService, SystemHealth, SystemInfo, SystemOverview } from '../services/adminService';

const statusBadgeStyle = (status: string): React.CSSProperties => {
  const normalized = status.toUpperCase();
  if (normalized === 'UP') {
    return { background: 'rgba(22,163,74,0.12)', color: '#16a34a', fontWeight: 700, padding: '0.15rem 0.5rem', borderRadius: '999px', fontSize: '0.78rem' };
  }
  if (normalized === 'DEGRADED') {
    return { background: 'rgba(217,119,6,0.14)', color: '#d97706', fontWeight: 700, padding: '0.15rem 0.5rem', borderRadius: '999px', fontSize: '0.78rem' };
  }
  return { background: 'rgba(220,38,38,0.12)', color: '#dc2626', fontWeight: 700, padding: '0.15rem 0.5rem', borderRadius: '999px', fontSize: '0.78rem' };
};

export const AdminSystemInfoPage: React.FC = () => {
  const [info, setInfo] = useState<SystemInfo | null>(null);
  const [health, setHealth] = useState<SystemHealth | null>(null);
  const [overview, setOverview] = useState<SystemOverview | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  const loadAll = useCallback(async () => {
    setLoading(true);
    setError(null);
    try {
      const [infoRes, healthRes, overviewRes] = await Promise.all([
        adminService.getSystemInfo(),
        adminService.getSystemHealth(),
        adminService.getSystemOverview(),
      ]);
      setInfo(infoRes);
      setHealth(healthRes);
      setOverview(overviewRes);
    } catch {
      setError('Não foi possível carregar os diagnósticos do sistema.');
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => {
    loadAll();
  }, [loadAll]);

  return (
    <AdminLayout>
      <div style={{ display: 'flex', flexDirection: 'column', gap: '1.25rem' }}>
        <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', flexWrap: 'wrap', gap: '0.75rem' }}>
          <div>
            <h1 style={{ fontSize: '1.5rem', marginBottom: '0.25rem', display: 'flex', alignItems: 'center', gap: '0.5rem' }}>
              <Activity size={22} color="var(--primary)" />
              <span>Info do Sistema & Saúde</span>
            </h1>
            <p style={{ color: 'var(--text-muted)', fontSize: '0.925rem', margin: 0 }}>
              Diagnóstico em tempo real da infraestrutura, jobs e métricas operacionais.
            </p>
          </div>

          <div style={{ display: 'flex', gap: '0.5rem', flexWrap: 'wrap' }}>
            <a
              href="http://localhost:9090"
              target="_blank"
              rel="noreferrer"
              className="btn btn-secondary"
              style={{ width: 'auto', gap: '0.35rem' }}
            >
              <ExternalLink size={14} />
              <span>Prometheus</span>
            </a>
            <a
              href="http://localhost:3001"
              target="_blank"
              rel="noreferrer"
              className="btn btn-secondary"
              style={{ width: 'auto', gap: '0.35rem' }}
            >
              <ExternalLink size={14} />
              <span>Grafana (Phase 3)</span>
            </a>
            <button onClick={loadAll} disabled={loading} className="btn btn-primary" style={{ width: 'auto', gap: '0.35rem' }}>
              <RefreshCw size={14} />
              <span>Atualizar</span>
            </button>
          </div>
        </div>

        <AlertError message={error} />

        {loading && !health ? (
          <div className="card" style={{ padding: '1.5rem' }}>
            <p style={{ color: 'var(--text-muted)' }}>Coletando métricas e diagnósticos...</p>
          </div>
        ) : (
          <>
            {health && (
              <div style={{ display: 'flex', flexDirection: 'column', gap: '0.75rem' }}>
                <div style={{ display: 'flex', alignItems: 'center', gap: '0.75rem' }}>
                  <h2 style={{ fontSize: '1.1rem', margin: 0 }}>Saúde da Infraestrutura</h2>
                  <span style={statusBadgeStyle(health.overallStatus)}>Geral: {health.overallStatus}</span>
                </div>

                <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(230px, 1fr))', gap: '1rem' }}>
                  <div className="card" style={{ padding: '1rem' }}>
                    <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '0.5rem' }}>
                      <strong style={{ display: 'flex', alignItems: 'center', gap: '0.4rem' }}>
                        <Database size={16} /> PostgreSQL
                      </strong>
                      <span style={statusBadgeStyle(health.database.status)}>{health.database.status}</span>
                    </div>
                    <div style={{ fontSize: '0.84rem', color: 'var(--text-muted)' }}>
                      <div>Latência: {health.database.latencyMs}ms</div>
                      <div>Pool HikariCP: {health.database.activeConnections} ativas / {health.database.totalConnections} total (máx {health.database.maxPoolSize})</div>
                      {health.database.error && <div style={{ color: '#dc2626', marginTop: '0.25rem' }}>{health.database.error}</div>}
                    </div>
                  </div>

                  <div className="card" style={{ padding: '1rem' }}>
                    <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '0.5rem' }}>
                      <strong style={{ display: 'flex', alignItems: 'center', gap: '0.4rem' }}>
                        <Server size={16} /> Redis
                      </strong>
                      <span style={statusBadgeStyle(health.redis.status)}>{health.redis.status}</span>
                    </div>
                    <div style={{ fontSize: '0.84rem', color: 'var(--text-muted)' }}>
                      <div>Latência PING: {health.redis.latencyMs}ms</div>
                      <div>Memória: {health.redis.usedMemory || 'N/A'} | Chaves: {health.redis.cachedKeys ?? 'N/A'}</div>
                      {health.redis.error && <div style={{ color: '#dc2626', marginTop: '0.25rem' }}>{health.redis.error}</div>}
                    </div>
                  </div>

                  <div className="card" style={{ padding: '1rem' }}>
                    <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '0.5rem' }}>
                      <strong style={{ display: 'flex', alignItems: 'center', gap: '0.4rem' }}>
                        <HardDrive size={16} /> Object Storage
                      </strong>
                      <span style={statusBadgeStyle(health.objectStorage.status)}>{health.objectStorage.status}</span>
                    </div>
                    <div style={{ fontSize: '0.84rem', color: 'var(--text-muted)' }}>
                      <div>Provider: {health.objectStorage.provider} ({health.objectStorage.bucketOrPath})</div>
                      <div>Latência: {health.objectStorage.latencyMs}ms</div>
                      {health.objectStorage.error && <div style={{ color: '#dc2626', marginTop: '0.25rem' }}>{health.objectStorage.error}</div>}
                    </div>
                  </div>

                  <div className="card" style={{ padding: '1rem' }}>
                    <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '0.5rem' }}>
                      <strong>Outbox & Jobs</strong>
                      <span style={statusBadgeStyle(health.outboxAndJobs.status)}>{health.outboxAndJobs.status}</span>
                    </div>
                    <div style={{ fontSize: '0.84rem', color: 'var(--text-muted)' }}>
                      <div>Outbox pendentes: {health.outboxAndJobs.pendingOutboxEvents} | Falhas: {health.outboxAndJobs.failedOutboxEvents}</div>
                      {health.outboxAndJobs.recentJobs.map((j) => (
                        <div key={j.jobName}>{j.jobName}: {j.status}</div>
                      ))}
                    </div>
                  </div>
                </div>
              </div>
            )}

            {overview && (
              <div className="card" style={{ padding: '1.25rem' }}>
                <div style={{ display: 'flex', alignItems: 'center', gap: '0.5rem', marginBottom: '0.75rem' }}>
                  <BarChart3 size={18} color="var(--primary)" />
                  <h2 style={{ fontSize: '1.1rem', margin: 0 }}>Indicadores da Plataforma</h2>
                </div>
                <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(160px, 1fr))', gap: '1rem' }}>
                  <div><div style={{ fontSize: '0.8rem', color: 'var(--text-muted)' }}>Total de Contas</div><div style={{ fontSize: '1.25rem', fontWeight: 700 }}>{overview.totalUsers}</div></div>
                  <div><div style={{ fontSize: '0.8rem', color: 'var(--text-muted)' }}>Contas Ativas</div><div style={{ fontSize: '1.25rem', fontWeight: 700 }}>{overview.activeUsers}</div></div>
                  <div><div style={{ fontSize: '0.8rem', color: 'var(--text-muted)' }}>Suspensas</div><div style={{ fontSize: '1.25rem', fontWeight: 700 }}>{overview.suspendedUsers}</div></div>
                  <div><div style={{ fontSize: '0.8rem', color: 'var(--text-muted)' }}>Novas (24h)</div><div style={{ fontSize: '1.25rem', fontWeight: 700 }}>{overview.newUsersLast24h}</div></div>
                  <div><div style={{ fontSize: '0.8rem', color: 'var(--text-muted)' }}>Comunidades Ativas</div><div style={{ fontSize: '1.25rem', fontWeight: 700 }}>{overview.activeCommunities}</div></div>
                  <div><div style={{ fontSize: '0.8rem', color: 'var(--text-muted)' }}>Check-ins (24h)</div><div style={{ fontSize: '1.25rem', fontWeight: 700 }}>{overview.checkinsLast24h}</div></div>
                  <div><div style={{ fontSize: '0.8rem', color: 'var(--text-muted)' }}>Premium Ativos</div><div style={{ fontSize: '1.25rem', fontWeight: 700 }}>{overview.activePremiumSubscriptions}</div></div>
                  <div><div style={{ fontSize: '0.8rem', color: 'var(--text-muted)' }}>Denúncias Pendentes</div><div style={{ fontSize: '1.25rem', fontWeight: 700 }}>{overview.pendingReports}</div></div>
                </div>
              </div>
            )}

            {info && (
              <div className="card" style={{ padding: '1.25rem' }}>
                <h2 style={{ fontSize: '1.1rem', marginBottom: '0.75rem' }}>Metadados da Aplicação</h2>
                <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(180px, 1fr))', gap: '0.75rem', fontSize: '0.875rem' }}>
                  <div><strong>Aplicação:</strong> {info.applicationName}</div>
                  <div><strong>Versão:</strong> {info.version}</div>
                  <div><strong>Commit:</strong> {info.commitSha}</div>
                  <div><strong>Perfis:</strong> {info.activeProfiles.join(', ')}</div>
                  <div><strong>JVM:</strong> Java {info.javaVersion}</div>
                  <div><strong>Uptime:</strong> {info.uptimeSeconds}s</div>
                  <div><strong>Flyway:</strong> {info.flyway.enabled ? `v${info.flyway.currentVersion} (${info.flyway.appliedMigrations} aplicadas)` : 'Desabilitado'}</div>
                </div>
              </div>
            )}
          </>
        )}
      </div>
    </AdminLayout>
  );
};
