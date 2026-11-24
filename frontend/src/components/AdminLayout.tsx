import React from 'react';
import { Link, useLocation } from 'react-router-dom';
import {
  ShieldCheck,
  Users,
  Lock,
  FileText,
  Activity,
  ArrowLeft,
  LogOut,
  ClipboardList,
} from 'lucide-react';
import { useAuth } from '../hooks/useAuth';

interface AdminLayoutProps {
  children: React.ReactNode;
}

export const AdminLayout: React.FC<AdminLayoutProps> = ({ children }) => {
  const { user, logout } = useAuth();
  const location = useLocation();

  const navItems = [
    { path: '/admin', label: 'Visão Geral & Segurança', icon: ShieldCheck, enabled: true },
    { path: '/admin/users', label: 'Contas da Plataforma', icon: Users, enabled: true },
    { path: '/admin/logs', label: 'Logs do Sistema', icon: FileText, enabled: true },
    { path: '/admin/audit', label: 'Auditoria', icon: ClipboardList, enabled: false, tag: 'Sprint 29' },
    { path: '/admin/system', label: 'Info do Sistema', icon: Activity, enabled: false, tag: 'Sprint 30' },
  ];

  return (
    <div style={{ minHeight: '100vh', display: 'flex', flexDirection: 'column', background: 'var(--bg)' }}>
      <header className="navbar" style={{ borderBottom: '1px solid var(--border)' }}>
        <div className="navbar-inner">
          <div style={{ display: 'flex', alignItems: 'center', gap: '1rem' }}>
            <Link to="/admin" className="navbar-brand" style={{ display: 'flex', alignItems: 'center', gap: '0.5rem' }}>
              <Lock size={22} color="var(--primary)" />
              <span>StudyWithUs Backoffice</span>
            </Link>
            <span
              className="user-badge"
              style={{ fontSize: '0.75rem', fontWeight: 700, letterSpacing: '0.03em' }}
            >
              {user?.globalRole || 'ADMIN'}
            </span>
          </div>

          <div className="navbar-user" style={{ display: 'flex', alignItems: 'center', gap: '0.75rem' }}>
            <Link
              to="/"
              className="btn btn-secondary"
              style={{ width: 'auto', padding: '0.4rem 0.8rem', fontSize: '0.85rem', gap: '0.4rem' }}
            >
              <ArrowLeft size={15} />
              <span>Voltar ao App</span>
            </Link>
            <button
              onClick={() => logout()}
              className="btn btn-secondary"
              style={{ width: 'auto', padding: '0.4rem 0.8rem', fontSize: '0.85rem', gap: '0.4rem' }}
            >
              <LogOut size={15} />
              <span>Sair</span>
            </button>
          </div>
        </div>
      </header>

      <div style={{ display: 'flex', flex: 1, maxWidth: '1280px', width: '100%', margin: '0 auto', padding: '1.5rem 1rem', gap: '1.5rem' }}>
        <aside style={{ width: '240px', flexShrink: 0 }}>
          <div className="card" style={{ padding: '0.75rem' }}>
            <nav style={{ display: 'flex', flexDirection: 'column', gap: '0.35rem' }}>
              {navItems.map((item) => {
                const Icon = item.icon;
                const active = location.pathname === item.path;
                if (!item.enabled) {
                  return (
                    <div
                      key={item.path}
                      style={{
                        display: 'flex',
                        alignItems: 'center',
                        justifyContent: 'space-between',
                        padding: '0.6rem 0.75rem',
                        borderRadius: '8px',
                        color: 'var(--text-muted)',
                        opacity: 0.65,
                        fontSize: '0.875rem',
                      }}
                    >
                      <span style={{ display: 'flex', alignItems: 'center', gap: '0.5rem' }}>
                        <Icon size={16} />
                        {item.label}
                      </span>
                      <span style={{ fontSize: '0.7rem', background: 'var(--bg)', padding: '0.1rem 0.35rem', borderRadius: '4px' }}>
                        {item.tag}
                      </span>
                    </div>
                  );
                }

                return (
                  <Link
                    key={item.path}
                    to={item.path}
                    style={{
                      display: 'flex',
                      alignItems: 'center',
                      gap: '0.5rem',
                      padding: '0.6rem 0.75rem',
                      borderRadius: '8px',
                      fontWeight: active ? 600 : 500,
                      fontSize: '0.9rem',
                      color: active ? 'var(--primary)' : 'var(--text)',
                      background: active ? 'rgba(99, 102, 241, 0.1)' : 'transparent',
                    }}
                  >
                    <Icon size={16} />
                    {item.label}
                  </Link>
                );
              })}
            </nav>
          </div>
        </aside>

        <main style={{ flex: 1, minWidth: 0 }}>{children}</main>
      </div>
    </div>
  );
};
