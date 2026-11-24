import React, { useEffect, useState } from 'react';
import { ShieldCheck, KeyRound, Users, CheckCircle2 } from 'lucide-react';
import { AdminLayout } from '../components/AdminLayout';
import { AlertError } from '../components/AlertError';
import {
  adminService,
  AdminSessionInfo,
  AdminUserSummary,
  TwoFactorSetupResponse,
} from '../services/adminService';

export const AdminDashboardPage: React.FC = () => {
  const [session, setSession] = useState<AdminSessionInfo | null>(null);
  const [users, setUsers] = useState<AdminUserSummary[]>([]);
  const [totalUsers, setTotalUsers] = useState<number>(0);
  const [setupData, setSetupData] = useState<TwoFactorSetupResponse | null>(null);
  const [totpCode, setTotpCode] = useState('');
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [statusMessage, setStatusMessage] = useState<string | null>(null);

  const loadData = async () => {
    setLoading(true);
    setError(null);
    try {
      const [sessionInfo, usersPage] = await Promise.all([
        adminService.getSession(),
        adminService.listUsers(0, 10),
      ]);
      setSession(sessionInfo);
      setUsers(usersPage.content);
      setTotalUsers(usersPage.totalElements);
    } catch {
      setError('Não foi possível carregar os dados administrativos.');
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    loadData();
  }, []);

  const handleStart2fa = async () => {
    setError(null);
    setStatusMessage(null);
    try {
      const result = await adminService.setupTwoFactor();
      setSetupData(result);
    } catch {
      setError('Falha ao iniciar configuração de 2FA.');
    }
  };

  const handleVerify2fa = async (e: React.FormEvent) => {
    e.preventDefault();
    setError(null);
    setStatusMessage(null);
    try {
      await adminService.verifyTwoFactor(totpCode.trim());
      setSetupData(null);
      setTotpCode('');
      setStatusMessage('Autenticação em dois fatores (2FA/TOTP) ativada com sucesso.');
      await loadData();
    } catch {
      setError('Código TOTP inválido. Verifique o autenticador e tente novamente.');
    }
  };

  const handleDisable2fa = async (e: React.FormEvent) => {
    e.preventDefault();
    setError(null);
    setStatusMessage(null);
    try {
      await adminService.disableTwoFactor(totpCode.trim());
      setTotpCode('');
      setStatusMessage('2FA desativado para esta conta.');
      await loadData();
    } catch {
      setError('Não foi possível desativar o 2FA. Verifique o código informado.');
    }
  };

  return (
    <AdminLayout>
      <div style={{ display: 'flex', flexDirection: 'column', gap: '1.25rem' }}>
        <div>
          <h1 style={{ fontSize: '1.5rem', marginBottom: '0.25rem' }}>Painel Administrativo</h1>
          <p style={{ color: 'var(--text-muted)', fontSize: '0.925rem' }}>
            Identidade administrativa, RBAC global e segurança de sessão.
          </p>
        </div>

        <AlertError message={error} />

        {statusMessage && (
          <div
            className="card"
            style={{
              padding: '0.85rem 1rem',
              borderLeft: '4px solid #16a34a',
              display: 'flex',
              alignItems: 'center',
              gap: '0.5rem',
            }}
          >
            <CheckCircle2 size={18} color="#16a34a" />
            <span>{statusMessage}</span>
          </div>
        )}

        {loading ? (
          <div className="card" style={{ padding: '1.5rem' }}>
            <p style={{ color: 'var(--text-muted)' }}>Carregando informações da sessão...</p>
          </div>
        ) : (
          <>
            {session && (
              <div className="card" style={{ padding: '1.25rem' }}>
                <div style={{ display: 'flex', alignItems: 'center', gap: '0.5rem', marginBottom: '0.75rem' }}>
                  <ShieldCheck size={20} color="var(--primary)" />
                  <h2 style={{ fontSize: '1.1rem', margin: 0 }}>Sessão Administrativa Atual</h2>
                </div>
                <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(180px, 1fr))', gap: '1rem' }}>
                  <div>
                    <div style={{ fontSize: '0.8rem', color: 'var(--text-muted)' }}>Operador</div>
                    <div style={{ fontWeight: 600 }}>{session.name} (@{session.username})</div>
                  </div>
                  <div>
                    <div style={{ fontSize: '0.8rem', color: 'var(--text-muted)' }}>Papel Global</div>
                    <div style={{ fontWeight: 600 }}>{session.globalRole}</div>
                  </div>
                  <div>
                    <div style={{ fontSize: '0.8rem', color: 'var(--text-muted)' }}>TTL do Access Token</div>
                    <div style={{ fontWeight: 600 }}>{session.accessTokenTtlSeconds}s</div>
                  </div>
                  <div>
                    <div style={{ fontSize: '0.8rem', color: 'var(--text-muted)' }}>2FA / TOTP</div>
                    <div style={{ fontWeight: 600 }}>
                      {session.totpEnabled ? 'Ativo' : session.totpEligible ? 'Não configurado' : 'Não aplicável'}
                    </div>
                  </div>
                </div>
              </div>
            )}

            {session?.totpEligible && (
              <div className="card" style={{ padding: '1.25rem' }}>
                <div style={{ display: 'flex', alignItems: 'center', gap: '0.5rem', marginBottom: '0.75rem' }}>
                  <KeyRound size={20} color="var(--primary)" />
                  <h2 style={{ fontSize: '1.1rem', margin: 0 }}>Segurança 2FA (TOTP)</h2>
                </div>

                {!session.totpEnabled && !setupData && (
                  <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', gap: '1rem', flexWrap: 'wrap' }}>
                    <p style={{ color: 'var(--text-muted)', margin: 0, fontSize: '0.9rem' }}>
                      Contas ADMIN e SUPER_ADMIN suportam autenticação em dois fatores via aplicativo autenticador.
                    </p>
                    <button onClick={handleStart2fa} className="btn btn-primary" style={{ width: 'auto' }}>
                      Configurar 2FA
                    </button>
                  </div>
                )}

                {setupData && (
                  <form onSubmit={handleVerify2fa} style={{ display: 'flex', flexDirection: 'column', gap: '0.75rem' }}>
                    <p style={{ fontSize: '0.9rem', margin: 0 }}>
                      Adicione a chave secreta abaixo no seu aplicativo autenticador e informe o código de 6 dígitos:
                    </p>
                    <code style={{ padding: '0.6rem', background: 'var(--bg)', borderRadius: '6px', fontSize: '0.85rem', wordBreak: 'break-all' }}>
                      {setupData.secret}
                    </code>
                    <div style={{ display: 'flex', gap: '0.5rem', alignItems: 'center' }}>
                      <input
                        type="text"
                        className="form-control"
                        placeholder="Código 6 dígitos"
                        value={totpCode}
                        onChange={(e) => setTotpCode(e.target.value)}
                        style={{ maxWidth: '180px' }}
                      />
                      <button type="submit" className="btn btn-primary" style={{ width: 'auto' }}>
                        Ativar 2FA
                      </button>
                    </div>
                  </form>
                )}

                {session.totpEnabled && (
                  <form onSubmit={handleDisable2fa} style={{ display: 'flex', gap: '0.5rem', alignItems: 'center', flexWrap: 'wrap' }}>
                    <input
                      type="text"
                      className="form-control"
                      placeholder="Código TOTP para desativar"
                      value={totpCode}
                      onChange={(e) => setTotpCode(e.target.value)}
                      style={{ maxWidth: '220px' }}
                    />
                    <button type="submit" className="btn btn-secondary" style={{ width: 'auto' }}>
                      Desativar 2FA
                    </button>
                  </form>
                )}
              </div>
            )}

            <div className="card" style={{ padding: '1.25rem' }}>
              <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', marginBottom: '0.75rem' }}>
                <div style={{ display: 'flex', alignItems: 'center', gap: '0.5rem' }}>
                  <Users size={20} color="var(--primary)" />
                  <h2 style={{ fontSize: '1.1rem', margin: 0 }}>Contas Recentes ({totalUsers})</h2>
                </div>
              </div>
              <div style={{ overflowX: 'auto' }}>
                <table style={{ width: '100%', borderCollapse: 'collapse', fontSize: '0.875rem' }}>
                  <thead>
                    <tr style={{ textAlign: 'left', borderBottom: '1px solid var(--border)', color: 'var(--text-muted)' }}>
                      <th style={{ padding: '0.5rem' }}>Usuário</th>
                      <th style={{ padding: '0.5rem' }}>E-mail</th>
                      <th style={{ padding: '0.5rem' }}>Papel Global</th>
                      <th style={{ padding: '0.5rem' }}>Status</th>
                    </tr>
                  </thead>
                  <tbody>
                    {users.map((u) => (
                      <tr key={u.id} style={{ borderBottom: '1px solid var(--border)' }}>
                        <td style={{ padding: '0.5rem', fontWeight: 500 }}>{u.name} (@{u.username})</td>
                        <td style={{ padding: '0.5rem', color: 'var(--text-muted)' }}>{u.email}</td>
                        <td style={{ padding: '0.5rem' }}>{u.globalRole}</td>
                        <td style={{ padding: '0.5rem' }}>{u.status}</td>
                      </tr>
                    ))}
                  </tbody>
                </table>
              </div>
            </div>
          </>
        )}
      </div>
    </AdminLayout>
  );
};
