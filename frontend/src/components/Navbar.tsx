import React from 'react';
import { Link, useLocation } from 'react-router-dom';
import { BookOpen, LogOut, Award, User as UserIcon, Users } from 'lucide-react';
import { useAuth } from '../hooks/useAuth';

export const Navbar: React.FC = () => {
  const { user, logout } = useAuth();
  const location = useLocation();

  return (
    <header className="navbar">
      <div className="navbar-inner">
        <div style={{ display: 'flex', alignItems: 'center', gap: '2rem' }}>
          <Link to="/" className="navbar-brand">
            <BookOpen size={24} />
            <span>StudyWithUs</span>
          </Link>

          {user && (
            <nav style={{ display: 'flex', gap: '1rem' }}>
              <Link
                to="/"
                style={{
                  fontWeight: 500,
                  fontSize: '0.925rem',
                  color: location.pathname === '/' ? 'var(--primary)' : 'var(--text-muted)',
                }}
              >
                Início
              </Link>
              <Link
                to="/communities"
                style={{
                  fontWeight: 500,
                  fontSize: '0.925rem',
                  display: 'flex',
                  alignItems: 'center',
                  gap: '0.3rem',
                  color: location.pathname.startsWith('/communities') ? 'var(--primary)' : 'var(--text-muted)',
                }}
              >
                <Users size={16} />
                <span>Comunidades</span>
              </Link>
              <Link
                to="/profile"
                style={{
                  fontWeight: 500,
                  fontSize: '0.925rem',
                  display: 'flex',
                  alignItems: 'center',
                  gap: '0.3rem',
                  color: location.pathname.startsWith('/profile') ? 'var(--primary)' : 'var(--text-muted)',
                }}
              >
                <UserIcon size={16} />
                <span>Perfil</span>
              </Link>
            </nav>
          )}
        </div>

        {user && (
          <div className="navbar-user">
            <div style={{ display: 'flex', alignItems: 'center', gap: '0.4rem' }}>
              <UserIcon size={18} color="var(--text-muted)" />
              <span style={{ fontWeight: 600 }}>{user.name}</span>
              <span style={{ color: 'var(--text-muted)', fontSize: '0.85rem' }}>@{user.username}</span>
            </div>

            <span className="user-badge" style={{ display: 'flex', alignItems: 'center', gap: '0.3rem' }}>
              <Award size={14} />
              {user.lifetimePoints} pts
            </span>

            <button
              onClick={() => logout()}
              className="btn btn-secondary"
              style={{ width: 'auto', padding: '0.4rem 0.8rem', fontSize: '0.875rem', gap: '0.4rem' }}
              title="Encerrar sessão"
            >
              <LogOut size={16} />
              <span>Sair</span>
            </button>
          </div>
        )}
      </div>
    </header>
  );
};
