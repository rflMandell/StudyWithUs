import { describe, it, expect, vi } from 'vitest';
import { render, screen } from '@testing-library/react';
import { MemoryRouter, Routes, Route } from 'react-router-dom';
import { AdminProtectedRoute } from '../components/AdminProtectedRoute';
import { AuthContext, AuthContextType } from '../context/AuthContext';
import { User } from '../types/auth';

const baseUser: User = {
  id: '11111111-1111-1111-1111-111111111111',
  username: 'tester',
  name: 'Tester',
  email: 'tester@studywithus.com',
  status: 'ACTIVE',
  globalRole: 'USER',
  lifetimePoints: 0,
  createdAt: '2026-01-01T00:00:00Z',
};

const renderWithAuth = (authOverrides: Partial<AuthContextType>) => {
  const value: AuthContextType = {
    user: null,
    isAuthenticated: false,
    isLoading: false,
    error: null,
    login: vi.fn(),
    register: vi.fn(),
    logout: vi.fn(),
    updateUser: vi.fn(),
    clearError: vi.fn(),
    ...authOverrides,
  };

  return render(
    <AuthContext.Provider value={value}>
      <MemoryRouter initialEntries={['/admin']}>
        <Routes>
          <Route path="/login" element={<div>Login Page</div>} />
          <Route
            path="/admin"
            element={
              <AdminProtectedRoute>
                <div>Admin Secret Panel</div>
              </AdminProtectedRoute>
            }
          />
        </Routes>
      </MemoryRouter>
    </AuthContext.Provider>
  );
};

describe('AdminProtectedRoute', () => {
  it('redirects unauthenticated users to /login', () => {
    renderWithAuth({ isAuthenticated: false, user: null });
    expect(screen.getByText('Login Page')).toBeInTheDocument();
  });

  it('blocks regular USER accounts with Acesso Restrito', () => {
    renderWithAuth({ isAuthenticated: true, user: { ...baseUser, globalRole: 'USER' } });
    expect(screen.getByText('Acesso Restrito')).toBeInTheDocument();
    expect(screen.queryByText('Admin Secret Panel')).not.toBeInTheDocument();
  });

  it('renders admin content for ADMIN globalRole', () => {
    renderWithAuth({ isAuthenticated: true, user: { ...baseUser, globalRole: 'ADMIN' } });
    expect(screen.getByText('Admin Secret Panel')).toBeInTheDocument();
  });
});
