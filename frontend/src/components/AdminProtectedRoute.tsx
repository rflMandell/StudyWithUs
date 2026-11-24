import React from 'react';
import { Link, Navigate, useLocation } from 'react-router-dom';
import { ShieldAlert } from 'lucide-react';
import { useAuth } from '../hooks/useAuth';
import { isAdminRole } from '../services/adminService';

export const AdminProtectedRoute: React.FC<{ children: React.ReactElement }> = ({ children }) => {
  const { user, isAuthenticated, isLoading } = useAuth();
  const location = useLocation();

  if (isLoading) {
    return (
      <div style={{ minHeight: '100vh', display: 'flex', alignItems: 'center', justifyContent: 'center' }}>
        <p style={{ color: 'var(--text-muted)', fontSize: '1.1rem' }}>Validando sessão administrativa...</p>
      </div>
    );
  }

  if (!isAuthenticated) {
    return <Navigate to="/login" state={{ from: location }} replace />;
  }

  if (!isAdminRole(user?.globalRole)) {
    return (
      <div style={{ minHeight: '100vh', display: 'flex', alignItems: 'center', justifyContent: 'center', padding: '1.5rem' }}>
        <div className="card" style={{ maxWidth: '460px', width: '100%', textAlign: 'center', padding: '2rem' }}>
          <div style={{ display: 'flex', justifyContent: 'center', marginBottom: '1rem', color: 'var(--danger, #dc2626)' }}>
            <ShieldAlert size={44} />
          </div>
          <h2 style={{ marginBottom: '0.5rem' }}>Acesso Restrito</h2>
          <p style={{ color: 'var(--text-muted)', marginBottom: '1.5rem' }}>
            Esta área é exclusiva para administradores da plataforma (SUPPORT, ADMIN ou SUPER_ADMIN).
          </p>
          <Link to="/" className="btn btn-primary" style={{ display: 'inline-flex', width: 'auto' }}>
            Voltar ao Início
          </Link>
        </div>
      </div>
    );
  }

  return children;
};
