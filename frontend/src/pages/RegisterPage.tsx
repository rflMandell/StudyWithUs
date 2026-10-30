import React, { useState } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import { UserPlus, BookOpen } from 'lucide-react';
import { useAuth } from '../hooks/useAuth';
import { AlertError } from '../components/AlertError';

export const RegisterPage: React.FC = () => {
  const navigate = useNavigate();
  const { register, error, clearError, isLoading } = useAuth();

  const [username, setUsername] = useState('');
  const [name, setName] = useState('');
  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');
  const [institution, setInstitution] = useState('');
  const [course, setCourse] = useState('');
  const [localError, setLocalError] = useState<string | null>(null);

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setLocalError(null);
    clearError();

    if (!username.trim() || !name.trim() || !email.trim() || !password) {
      setLocalError('Por favor, preencha os campos obrigatórios (*).');
      return;
    }

    if (username.trim().length < 3) {
      setLocalError('O nome de usuário deve ter pelo menos 3 caracteres.');
      return;
    }

    if (password.length < 6) {
      setLocalError('A senha deve possuir no mínimo 6 caracteres.');
      return;
    }

    try {
      await register({
        username: username.trim(),
        name: name.trim(),
        email: email.trim(),
        password,
        institution: institution.trim() || undefined,
        course: course.trim() || undefined,
        deviceId: 'web-browser',
      });
      navigate('/');
    } catch {
      // Error handled by AuthContext
    }
  };

  return (
    <div className="auth-wrapper">
      <div className="auth-card" style={{ maxWidth: '480px' }}>
        <div className="auth-header">
          <div style={{ display: 'flex', justifyContent: 'center', marginBottom: '0.75rem', color: 'var(--primary)' }}>
            <BookOpen size={40} />
          </div>
          <h1>Crie sua Conta</h1>
          <p>Conecte-se com sua comunidade acadêmica</p>
        </div>

        <AlertError message={localError || error} />

        <form onSubmit={handleSubmit}>
          <div className="form-group">
            <label htmlFor="name">Nome Completo *</label>
            <input
              id="name"
              type="text"
              className="form-control"
              value={name}
              onChange={(e) => setName(e.target.value)}
              placeholder="Ex: Ana Clara Souza"
              disabled={isLoading}
            />
          </div>

          <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '0.75rem' }}>
            <div className="form-group">
              <label htmlFor="username">Usuário *</label>
              <input
                id="username"
                type="text"
                className="form-control"
                value={username}
                onChange={(e) => setUsername(e.target.value)}
                placeholder="anaclara"
                autoComplete="username"
                disabled={isLoading}
              />
            </div>

            <div className="form-group">
              <label htmlFor="email">E-mail *</label>
              <input
                id="email"
                type="email"
                className="form-control"
                value={email}
                onChange={(e) => setEmail(e.target.value)}
                placeholder="ana@faculdade.br"
                autoComplete="email"
                disabled={isLoading}
              />
            </div>
          </div>

          <div className="form-group">
            <label htmlFor="password">Senha (mínimo 6 caracteres) *</label>
            <input
              id="password"
              type="password"
              className="form-control"
              value={password}
              onChange={(e) => setPassword(e.target.value)}
              placeholder="••••••••"
              autoComplete="new-password"
              disabled={isLoading}
            />
          </div>

          <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '0.75rem' }}>
            <div className="form-group">
              <label htmlFor="institution">Instituição</label>
              <input
                id="institution"
                type="text"
                className="form-control"
                value={institution}
                onChange={(e) => setInstitution(e.target.value)}
                placeholder="Ex: USP, Unicamp, etc."
                disabled={isLoading}
              />
            </div>

            <div className="form-group">
              <label htmlFor="course">Curso</label>
              <input
                id="course"
                type="text"
                className="form-control"
                value={course}
                onChange={(e) => setCourse(e.target.value)}
                placeholder="Ex: Engenharia"
                disabled={isLoading}
              />
            </div>
          </div>

          <button
            type="submit"
            className="btn btn-primary"
            disabled={isLoading}
            style={{ marginTop: '0.75rem', gap: '0.5rem' }}
          >
            <UserPlus size={18} />
            <span>{isLoading ? 'Cadastrando...' : 'Criar Conta'}</span>
          </button>
        </form>

        <p style={{ textAlign: 'center', marginTop: '1.5rem', fontSize: '0.875rem', color: 'var(--text-muted)' }}>
          Já possui conta? <Link to="/login">Entrar</Link>
        </p>
      </div>
    </div>
  );
};
