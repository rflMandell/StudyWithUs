import { render, screen, waitFor, fireEvent } from '@testing-library/react';
import { MemoryRouter } from 'react-router-dom';
import { describe, it, expect, vi, beforeEach } from 'vitest';
import { SettingsPage } from '../pages/SettingsPage';
import { Navbar } from '../components/Navbar';
import { AuthContext, AuthContextType } from '../context/AuthContext';
import { ThemeProvider } from '../context/ThemeContext';
import { userService } from '../services/userService';
import { notificationService } from '../services/notificationService';
import { User } from '../types/auth';

const mockNavigate = vi.fn();

vi.mock('react-router-dom', async () => {
  const actual = await vi.importActual<typeof import('react-router-dom')>('react-router-dom');
  return {
    ...actual,
    useNavigate: () => mockNavigate,
  };
});

vi.mock('../services/userService', () => ({
  userService: {
    getProfile: vi.fn(),
    updateProfile: vi.fn(),
    uploadAvatar: vi.fn(),
    getUserStreak: vi.fn(),
    getUserBadges: vi.fn(),
    changePassword: vi.fn(),
    updatePrivacy: vi.fn(),
    deactivateAccount: vi.fn(),
    deleteAccount: vi.fn(),
  },
}));

vi.mock('../services/notificationService', () => ({
  notificationService: {
    getPreferences: vi.fn(),
    updatePreferences: vi.fn(),
  },
}));

const baseUser: User = {
  id: 'user-settings-1',
  username: 'joao_silva',
  name: 'João Silva',
  email: 'joao@usp.br',
  status: 'ACTIVE',
  isPrivate: false,
  lifetimePoints: 50,
  createdAt: '2026-10-01T10:00:00Z',
};

const defaultPreferences = {
  commentsEnabled: true,
  rankingEnabled: true,
  challengeEnabled: false,
  invitationEnabled: true,
  reminderEnabled: false,
  streakEnabled: true,
};

const createAuthContext = (overrides?: Partial<AuthContextType>): AuthContextType => ({
  user: baseUser,
  isAuthenticated: true,
  isLoading: false,
  error: null,
  login: vi.fn(),
  register: vi.fn(),
  logout: vi.fn().mockResolvedValue(undefined),
  updateUser: vi.fn(),
  clearError: vi.fn(),
  ...overrides,
});

const renderSettings = (authOverrides?: Partial<AuthContextType>) => {
  const authValue = createAuthContext(authOverrides);
  return {
    authValue,
    ...render(
      <ThemeProvider>
        <MemoryRouter initialEntries={['/settings']}>
          <AuthContext.Provider value={authValue}>
            <SettingsPage />
          </AuthContext.Provider>
        </MemoryRouter>
      </ThemeProvider>
    ),
  };
};

describe('SettingsPage', () => {
  beforeEach(() => {
    vi.clearAllMocks();
    localStorage.clear();
    document.documentElement.removeAttribute('data-theme');
    vi.mocked(notificationService.getPreferences).mockResolvedValue(defaultPreferences);
  });

  it('renders both main settings sections', async () => {
    renderSettings();

    expect(screen.getByRole('heading', { name: 'Configurações do Perfil' })).toBeInTheDocument();
    expect(screen.getByRole('heading', { name: 'Configurações Gerais' })).toBeInTheDocument();

    await waitFor(() => {
      expect(notificationService.getPreferences).toHaveBeenCalled();
    });
  });

  it('validates and changes password', async () => {
    vi.mocked(userService.changePassword).mockResolvedValue(undefined);
    renderSettings();

    fireEvent.change(screen.getByLabelText('Senha atual'), {
      target: { value: 'oldpass123' },
    });
    fireEvent.change(screen.getByLabelText('Nova senha'), {
      target: { value: 'newpass123' },
    });
    fireEvent.change(screen.getByLabelText('Confirmar nova senha'), {
      target: { value: 'mismatch123' },
    });

    fireEvent.click(screen.getByRole('button', { name: 'Atualizar Senha' }));
    expect(screen.getByText('A nova senha e a confirmação não coincidem.')).toBeInTheDocument();
    expect(userService.changePassword).not.toHaveBeenCalled();

    fireEvent.change(screen.getByLabelText('Confirmar nova senha'), {
      target: { value: 'newpass123' },
    });
    fireEvent.click(screen.getByRole('button', { name: 'Atualizar Senha' }));

    await waitFor(() => {
      expect(userService.changePassword).toHaveBeenCalledWith({
        currentPassword: 'oldpass123',
        newPassword: 'newpass123',
        confirmPassword: 'newpass123',
      });
      expect(screen.getByText('Senha alterada com sucesso!')).toBeInTheDocument();
    });
  });

  it('toggles account privacy and handles 429 rate-limit error', async () => {
    const updatedUser: User = { ...baseUser, isPrivate: true };
    vi.mocked(userService.updatePrivacy).mockResolvedValueOnce(updatedUser);

    const { authValue } = renderSettings();

    fireEvent.click(screen.getByRole('button', { name: /Conta Privada/i }));

    await waitFor(() => {
      expect(userService.updatePrivacy).toHaveBeenCalledWith(true);
      expect(authValue.updateUser).toHaveBeenCalledWith(updatedUser);
      expect(screen.getByText('Sua conta agora é Privada.')).toBeInTheDocument();
    });

    vi.mocked(userService.updatePrivacy).mockRejectedValueOnce({
      response: {
        status: 429,
        data: { message: 'Alteração permitida apenas 1 vez a cada 10 minutos.' },
      },
    });

    // Click Conta Privada again (user in context is still baseUser with isPrivate: false)
    fireEvent.click(screen.getByRole('button', { name: /Conta Privada/i }));

    await waitFor(() => {
      expect(
        screen.getByText('Alteração permitida apenas 1 vez a cada 10 minutos.')
      ).toBeInTheDocument();
    });
  });

  it('deactivates account after confirmation and logs out', async () => {
    vi.mocked(userService.deactivateAccount).mockResolvedValue(undefined);
    const { authValue } = renderSettings();

    fireEvent.click(screen.getByRole('button', { name: 'Desativar Conta' }));
    expect(
      screen.getByText('Tem certeza que deseja desativar sua conta?')
    ).toBeInTheDocument();

    fireEvent.click(screen.getByRole('button', { name: 'Confirmar Desativação' }));

    await waitFor(() => {
      expect(userService.deactivateAccount).toHaveBeenCalled();
      expect(authValue.logout).toHaveBeenCalled();
      expect(mockNavigate).toHaveBeenCalledWith('/login');
    });
  });

  it('deletes account after typing EXCLUIR and logs out', async () => {
    vi.mocked(userService.deleteAccount).mockResolvedValue(undefined);
    const { authValue } = renderSettings();

    fireEvent.click(screen.getByRole('button', { name: 'Excluir Conta Permanentemente' }));

    const confirmInput = screen.getByLabelText('Confirmar exclusão digitando EXCLUIR');
    fireEvent.change(confirmInput, { target: { value: 'EXCLUIR' } });
    fireEvent.click(screen.getByRole('button', { name: 'Confirmar Exclusão' }));

    await waitFor(() => {
      expect(userService.deleteAccount).toHaveBeenCalled();
      expect(authValue.logout).toHaveBeenCalled();
      expect(mockNavigate).toHaveBeenCalledWith('/login');
    });
  });

  it('switches theme mode and updates data-theme attribute', async () => {
    renderSettings();

    fireEvent.click(screen.getByRole('button', { name: /Escuro/i }));
    expect(document.documentElement.getAttribute('data-theme')).toBe('dark');
    expect(localStorage.getItem('theme_mode')).toBe('dark');

    fireEvent.click(screen.getByRole('button', { name: /Claro/i }));
    expect(document.documentElement.getAttribute('data-theme')).toBe('light');
    expect(localStorage.getItem('theme_mode')).toBe('light');

    fireEvent.click(screen.getByRole('button', { name: /Sistema operacional/i }));
    expect(document.documentElement.getAttribute('data-theme')).toBe('light');
    expect(localStorage.getItem('theme_mode')).toBe('system');
  });

  it('loads and updates notification preferences', async () => {
    vi.mocked(notificationService.updatePreferences).mockResolvedValue({
      ...defaultPreferences,
      challengeEnabled: true,
    });

    renderSettings();

    const challengeCheckbox = await screen.findByLabelText(/Desafios e metas/i);
    expect(challengeCheckbox).not.toBeChecked();

    fireEvent.click(challengeCheckbox);
    expect(challengeCheckbox).toBeChecked();

    fireEvent.click(screen.getByRole('button', { name: 'Salvar Preferências' }));

    await waitFor(() => {
      expect(notificationService.updatePreferences).toHaveBeenCalledWith({
        ...defaultPreferences,
        challengeEnabled: true,
      });
      expect(
        screen.getByText('Preferências de notificação salvas com sucesso!')
      ).toBeInTheDocument();
    });
  });

  it('switches between Terms of Use, Privacy Policy, Cookie Policy, and LGPD tabs', () => {
    renderSettings();

    expect(screen.getByRole('heading', { name: 'Termos de Uso' })).toBeInTheDocument();

    fireEvent.click(screen.getByRole('tab', { name: 'Política de Privacidade' }));
    expect(screen.getByRole('heading', { name: 'Política de Privacidade' })).toBeInTheDocument();

    fireEvent.click(screen.getByRole('tab', { name: 'Política de Cookies' }));
    expect(screen.getByRole('heading', { name: 'Política de Cookies' })).toBeInTheDocument();

    fireEvent.click(screen.getByRole('tab', { name: 'Diretrizes da Comunidade e LGPD' }));
    expect(
      screen.getByRole('heading', { name: 'Diretrizes da Comunidade e LGPD' })
    ).toBeInTheDocument();
  });
});

describe('Navbar Configurações link', () => {
  it('renders Configurações link pointing to /settings', () => {
    const authValue = createAuthContext();
    render(
      <MemoryRouter initialEntries={['/']}>
        <AuthContext.Provider value={authValue}>
          <Navbar />
        </AuthContext.Provider>
      </MemoryRouter>
    );

    const settingsLink = screen.getByRole('link', { name: /Configurações/i });
    expect(settingsLink).toBeInTheDocument();
    expect(settingsLink).toHaveAttribute('href', '/settings');
  });
});
