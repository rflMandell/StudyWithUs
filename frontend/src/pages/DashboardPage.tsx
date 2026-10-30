import React from 'react';
import { Award, School, Sparkles, CheckCircle2 } from 'lucide-react';
import { useAuth } from '../hooks/useAuth';
import { Navbar } from '../components/Navbar';

export const DashboardPage: React.FC = () => {
  const { user } = useAuth();

  if (!user) return null;

  return (
    <div>
      <Navbar />
      <main className="container">
        <div style={{ marginBottom: '2rem' }}>
          <h1 style={{ fontSize: '2rem', fontWeight: 700, letterSpacing: '-0.025em' }}>
            Olá, {user.name}! 👋
          </h1>
          <p style={{ color: 'var(--text-muted)', marginTop: '0.25rem' }}>
            Bem-vindo(a) ao StudyWithUs. Sua consistência acadêmica começa aqui.
          </p>
        </div>

        <div className="grid-cards">
          <div className="card">
            <div style={{ display: 'flex', alignItems: 'center', gap: '0.6rem', color: 'var(--primary)', marginBottom: '0.5rem' }}>
              <Award size={22} />
              <h3>Pontuação Acumulada</h3>
            </div>
            <p style={{ fontSize: '2.25rem', fontWeight: 700, color: 'var(--primary)' }}>
              {user.lifetimePoints} <span style={{ fontSize: '1rem', fontWeight: 500, color: 'var(--text-muted)' }}>pontos</span>
            </p>
            <p style={{ fontSize: '0.85rem', color: 'var(--text-muted)', marginTop: '0.5rem' }}>
              Ganhe pontos registrando check-ins e participando de desafios.
            </p>
          </div>

          <div className="card">
            <div style={{ display: 'flex', alignItems: 'center', gap: '0.6rem', color: '#0284c7', marginBottom: '0.5rem' }}>
              <School size={22} />
              <h3>Dados Acadêmicos</h3>
            </div>
            <p style={{ fontWeight: 600 }}>{user.institution || 'Instituição não informada'}</p>
            <p style={{ color: 'var(--text-muted)', fontSize: '0.9rem' }}>{user.course || 'Curso não informado'}</p>
            <div style={{ marginTop: '0.75rem', display: 'flex', alignItems: 'center', gap: '0.4rem', color: '#16a34a', fontSize: '0.85rem', fontWeight: 500 }}>
              <CheckCircle2 size={16} />
              <span>Conta Verificada e Ativa</span>
            </div>
          </div>

          <div className="card">
            <div style={{ display: 'flex', alignItems: 'center', gap: '0.6rem', color: '#d97706', marginBottom: '0.5rem' }}>
              <Sparkles size={22} />
              <h3>Próximos Passos</h3>
            </div>
            <ul style={{ listStyle: 'none', fontSize: '0.9rem', color: 'var(--text-muted)', display: 'flex', flexDirection: 'column', gap: '0.4rem' }}>
              <li>• Entrar em comunidades de estudo (Sprint 3)</li>
              <li>• Registrar seus primeiros check-ins (Sprint 5)</li>
              <li>• Subir no ranking acadêmico semanal (Sprint 7)</li>
            </ul>
          </div>
        </div>
      </main>
    </div>
  );
};
