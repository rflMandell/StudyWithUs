import { render, screen, fireEvent, waitFor } from '@testing-library/react';
import { BrowserRouter } from 'react-router-dom';
import { describe, it, expect, vi, beforeEach } from 'vitest';
import { LoginPage } from '../pages/LoginPage';
import { AuthContext, AuthContextType } from '../context/AuthContext';

const mockLogin = vi.fn();
const mockClearError = vi.fn();

const defaultContext: AuthContextType = {
  user: null,
  isAuthenticated: false,
  isLoading: false,
  error: null,
  login: mockLogin,
  register: vi.fn(),
  logout: vi.fn(),
  clearError: mockClearError,
};

const renderLoginPage = (contextValue = defaultContext) => {
  return render(
    <BrowserRouter>
      <AuthContext.Provider value={contextValue}>
        <LoginPage />
      </AuthContext.Provider>
    </BrowserRouter>
  );
};

describe('LoginPage', () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  it('renders login form with all inputs and submit button', () => {
    renderLoginPage();

    expect(screen.getByText('StudyWithUs')).toBeInTheDocument();
    expect(screen.getByLabelText(/usuário ou e-mail/i)).toBeInTheDocument();
    expect(screen.getByLabelText(/senha/i)).toBeInTheDocument();
    expect(screen.getByRole('button', { name: /entrar/i })).toBeInTheDocument();
    expect(screen.getByRole('link', { name: /cadastre-se/i })).toBeInTheDocument();
  });

  it('shows error message when submitted empty', async () => {
    renderLoginPage();

    const submitBtn = screen.getByRole('button', { name: /entrar/i });
    fireEvent.click(submitBtn);

    await waitFor(() => {
      expect(screen.getByText(/por favor, informe usuário\/e-mail e senha/i)).toBeInTheDocument();
    });

    expect(mockLogin).not.toHaveBeenCalled();
  });

  it('calls login when valid credentials are submitted', async () => {
    mockLogin.mockResolvedValueOnce(undefined);
    renderLoginPage();

    fireEvent.change(screen.getByLabelText(/usuário ou e-mail/i), {
      target: { value: 'alice' },
    });
    fireEvent.change(screen.getByLabelText(/senha/i), {
      target: { value: 'password123' },
    });

    fireEvent.click(screen.getByRole('button', { name: /entrar/i }));

    await waitFor(() => {
      expect(mockLogin).toHaveBeenCalledWith({
        usernameOrEmail: 'alice',
        password: 'password123',
        deviceId: 'web-browser',
      });
    });
  });
});
