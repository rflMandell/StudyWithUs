import { renderHook, act } from '@testing-library/react';
import { describe, it, expect, vi, beforeEach } from 'vitest';
import { AuthProvider, AuthContext } from '../context/AuthContext';
import { authService } from '../services/authService';
import { realtimeService } from '../services/realtimeService';
import { useContext } from 'react';
import { User, AuthResponse } from '../types/auth';
import { RealtimeNotification } from '../types/realtime';

vi.mock('../services/authService', () => ({
  authService: {
    getStoredAccessToken: vi.fn(),
    getStoredRefreshToken: vi.fn(),
    setTokens: vi.fn(),
    clearTokens: vi.fn(),
    login: vi.fn(),
    register: vi.fn(),
    logout: vi.fn(),
    getMe: vi.fn(),
  },
}));

const mockUser: User = {
  id: 'user-1',
  username: 'carlos',
  name: 'Carlos Alberto',
  email: 'carlos@ufmg.br',
  status: 'ACTIVE',
  lifetimePoints: 120,
  createdAt: '2026-10-30T10:00:00Z',
};

const mockAuthResponse: AuthResponse = {
  accessToken: 'jwt-access-token',
  refreshToken: 'jwt-refresh-token',
  tokenType: 'Bearer',
  expiresIn: 900,
  userId: 'user-1',
  username: 'carlos',
  email: 'carlos@ufmg.br',
  name: 'Carlos Alberto',
};

describe('AuthContext', () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  it('restores user session when token exists', async () => {
    vi.mocked(authService.getStoredAccessToken).mockReturnValue('stored-token');
    vi.mocked(authService.getMe).mockResolvedValue(mockUser);

    const { result } = renderHook(() => useContext(AuthContext), {
      wrapper: AuthProvider,
    });

    await act(async () => {});

    expect(result.current?.isAuthenticated).toBe(true);
    expect(result.current?.user?.username).toBe('carlos');
    expect(result.current?.user?.lifetimePoints).toBe(120);
  });

  it('logs in user successfully', async () => {
    vi.mocked(authService.getStoredAccessToken).mockReturnValue(null);
    vi.mocked(authService.login).mockResolvedValue(mockAuthResponse);
    vi.mocked(authService.getMe).mockResolvedValue(mockUser);

    const { result } = renderHook(() => useContext(AuthContext), {
      wrapper: AuthProvider,
    });

    await act(async () => {});

    await act(async () => {
      await result.current?.login({
        usernameOrEmail: 'carlos',
        password: 'password123',
      });
    });

    expect(authService.setTokens).toHaveBeenCalledWith('jwt-access-token', 'jwt-refresh-token');
    expect(result.current?.isAuthenticated).toBe(true);
    expect(result.current?.user?.name).toBe('Carlos Alberto');
  });

  it('logs out user and clears tokens', async () => {
    vi.mocked(authService.getStoredAccessToken).mockReturnValue('stored-token');
    vi.mocked(authService.getMe).mockResolvedValue(mockUser);
    vi.mocked(authService.logout).mockResolvedValue(undefined);

    const { result } = renderHook(() => useContext(AuthContext), {
      wrapper: AuthProvider,
    });

    await act(async () => {});
    expect(result.current?.isAuthenticated).toBe(true);

    await act(async () => {
      await result.current?.logout();
    });

    expect(authService.logout).toHaveBeenCalled();
    expect(result.current?.isAuthenticated).toBe(false);
    expect(result.current?.user).toBeNull();
  });

  it('refreshes user points via refreshUser without losing session', async () => {
    vi.mocked(authService.getStoredAccessToken).mockReturnValue('stored-token');
    vi.mocked(authService.getMe)
      .mockResolvedValueOnce(mockUser)
      .mockResolvedValueOnce({ ...mockUser, lifetimePoints: 130 });

    const { result } = renderHook(() => useContext(AuthContext), {
      wrapper: AuthProvider,
    });

    await act(async () => {});
    expect(result.current?.user?.lifetimePoints).toBe(120);

    await act(async () => {
      await result.current?.refreshUser?.();
    });

    expect(result.current?.user?.lifetimePoints).toBe(130);
  });

  it('automatically updates user points when POINTS_AWARDED realtime notification arrives', async () => {
    let listener: ((notification: RealtimeNotification) => void) | null = null;
    const subscribeSpy = vi
      .spyOn(realtimeService, 'subscribe')
      .mockImplementation((cb) => {
        listener = cb;
        return () => {
          listener = null;
        };
      });
    const connectSpy = vi.spyOn(realtimeService, 'connect').mockImplementation(() => {});

    vi.mocked(authService.getStoredAccessToken).mockReturnValue('stored-token');
    vi.mocked(authService.getMe)
      .mockResolvedValueOnce(mockUser)
      .mockResolvedValueOnce({ ...mockUser, lifetimePoints: 130 });

    const { result } = renderHook(() => useContext(AuthContext), {
      wrapper: AuthProvider,
    });

    await act(async () => {});
    expect(result.current?.user?.lifetimePoints).toBe(120);
    expect(listener).not.toBeNull();

    await act(async () => {
      listener!({
        id: 'notif-pts-1',
        type: 'POINTS_AWARDED',
        recipientUserId: 'user-1',
        title: 'Pontos recebidos!',
        message: 'Você ganhou +10 pontos!',
        data: { amount: 10 },
        timestamp: new Date().toISOString(),
      });
    });

    expect(result.current?.user?.lifetimePoints).toBe(130);

    subscribeSpy.mockRestore();
    connectSpy.mockRestore();
  });
});
