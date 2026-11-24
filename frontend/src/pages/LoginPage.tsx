import React, { useState } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import { AxiosError } from 'axios';
import { LogIn, BookOpen } from 'lucide-react';
import { useAuth } from '../hooks/useAuth';
import { AlertError } from '../components/AlertError';
import { ApiError } from '../types/auth';

export const LoginPage: React.FC = () => {
  const navigate = useNavigate();
  const { login, error, clearError, isLoading } = useAuth();

  const [usernameOrEmail, setUsernameOrEmail] = useState('');
  const [password, setPassword] = useState('');
  const [totpCode, setTotpCode] = useState('');
  const [requiresTotp, setRequiresTotp] = useState(false);
  const [localError, setLocalError] = useState<string | null>(null);

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setLocalError(null);
    clearError();

    if (!usernameOrEmail.trim() || !password) {
      setLocalError('Por favor, informe usuário/e-mail e senha.');
      return;
    }

    try {
      await login({
        usernameOrEmail: usernameOrEmail.trim(),
        password,
        deviceId: 'web-browser',
        ...(totpCode.trim() ? { totpCode: totpCode.trim() } : {}),
      });
      navigate('/');
    } catch (err) {
      if (err instanceof AxiosError && err.response?.data) {
        const apiErr = err.response.data as ApiError;
        if (apiErr.code === 'TOTP_REQUIRED' || apiErr.code === 'INVALID_TOTP_CODE') {
          setRequiresTotp(true);
        }
      }
    }
  };

  return (
    <div className="auth-wrapper">
      <div className="auth-card">
        <div className="auth-header">
          <div style={{ display: 'flex', justifyContent: 'center', marginBottom: '0.75rem', color: 'var(--primary)' }}>
            <BookOpen size={40} />
          </div>
          <h1>StudyWithUs</h1>
          <p>Entre para continuar sua jornada acadêmica</p>
        </div>

        <AlertError message={localError || error} />

        <form onSubmit={handleSubmit}>
          <div className="form-group">
            <label htmlFor="usernameOrEmail">Usuário ou E-mail</label>
            <input
              id="usernameOrEmail"
              type="text"
              className="form-control"
              value={usernameOrEmail}
              onChange={(e) => setUsernameOrEmail(e.target.value)}
              placeholder="seu_usuario ou email@exemplo.com"
              autoComplete="username"
              disabled={isLoading}
            />
          </div>

          <div className="form-group">
            <label htmlFor="password">Senha</label>
            <input
              id="password"
              type="password"
              className="form-control"
              value={password}
              onChange={(e) => setPassword(e.target.value)}
              placeholder="••••••••"
              autoComplete="current-password"
              disabled={isLoading}
            />
          </div>

          {requiresTotp && (
            <div className="form-group">
              <label htmlFor="totpCode">Código 2FA (TOTP)</label>
              <input
                id="totpCode"
                type="text"
                className="form-control"
                value={totpCode}
                onChange={(e) => setTotpCode(e.target.value)}
                placeholder="000000"
                autoComplete="one-time-code"
                disabled={isLoading}
              />
            </div>
          )}

          <button
            type="submit"
            className="btn btn-primary"
            disabled={isLoading}
            style={{ marginTop: '0.75rem', gap: '0.5rem' }}
          >
            <LogIn size={18} />
            <span>{isLoading ? 'Entrando...' : 'Entrar'}</span>
          </button>
        </form>

        <p style={{ textAlign: 'center', marginTop: '1.5rem', fontSize: '0.875rem', color: 'var(--text-muted)' }}>
          Ainda não tem conta? <Link to="/register">Cadastre-se</Link>
        </p>
      </div>
    </div>
  );
};
