import React, { createContext, useCallback, useEffect, useState } from 'react';
import { AxiosError } from 'axios';
import { ApiError, LoginCredentials, RegisterData, User } from '../types/auth';
import { authService } from '../services/authService';
import { realtimeService } from '../services/realtimeService';

export interface AuthContextType {
  user: User | null;
  isAuthenticated: boolean;
  isLoading: boolean;
  error: string | null;
  login: (credentials: LoginCredentials) => Promise<void>;
  register: (data: RegisterData) => Promise<void>;
  logout: () => Promise<void>;
  updateUser: (updatedUser: User) => void;
  refreshUser?: () => Promise<void>;
  clearError: () => void;
}

export const AuthContext = createContext<AuthContextType | undefined>(undefined);

export const AuthProvider: React.FC<{ children: React.ReactNode }> = ({ children }) => {
  const [user, setUser] = useState<User | null>(null);
  const [isLoading, setIsLoading] = useState<boolean>(true);
  const [error, setError] = useState<string | null>(null);

  const clearError = () => setError(null);

  const formatErrorMessage = (err: unknown): string => {
    if (err instanceof AxiosError && err.response?.data) {
      const apiErr = err.response.data as ApiError;
      return apiErr.message || apiErr.code || 'Ocorreu um erro na requisição.';
    }
    if (err instanceof Error) {
      return err.message;
    }
    return 'Erro inesperado. Tente novamente.';
  };

  const loadCurrentUser = useCallback(async () => {
    const token = authService.getStoredAccessToken();
    if (!token) {
      setUser(null);
      setIsLoading(false);
      return;
    }

    try {
      const userData = await authService.getMe();
      setUser(userData);
    } catch {
      authService.clearTokens();
      setUser(null);
    } finally {
      setIsLoading(false);
    }
  }, []);

  const refreshUser = useCallback(async () => {
    const token = authService.getStoredAccessToken();
    if (!token) {
      return;
    }

    try {
      const userData = await authService.getMe();
      setUser(userData);
    } catch {
      // Keep current user state if background refresh fails
    }
  }, []);

  useEffect(() => {
    loadCurrentUser();

    const handleUnauthorized = () => {
      realtimeService.disconnect();
      setUser(null);
    };

    window.addEventListener('auth:unauthorized', handleUnauthorized);
    return () => {
      window.removeEventListener('auth:unauthorized', handleUnauthorized);
    };
  }, [loadCurrentUser]);

  useEffect(() => {
    if (!user?.id) return;

    realtimeService.connect();

    const unsubscribe = realtimeService.subscribe((notification) => {
      if (notification.type === 'POINTS_AWARDED') {
        if (typeof notification.data?.lifetimePoints === 'number') {
          setUser((prev) =>
            prev ? { ...prev, lifetimePoints: notification.data.lifetimePoints } : prev
          );
        }
        void refreshUser();
      }
    });

    return () => {
      unsubscribe();
    };
  }, [user?.id, refreshUser]);

  const login = async (credentials: LoginCredentials) => {
    setIsLoading(true);
    setError(null);
    try {
      const response = await authService.login(credentials);
      authService.setTokens(response.accessToken, response.refreshToken);
      const userData = await authService.getMe();
      setUser(userData);
    } catch (err) {
      authService.clearTokens();
      const msg = formatErrorMessage(err);
      setError(msg);
      throw err;
    } finally {
      setIsLoading(false);
    }
  };

  const register = async (data: RegisterData) => {
    setIsLoading(true);
    setError(null);
    try {
      const response = await authService.register(data);
      authService.setTokens(response.accessToken, response.refreshToken);
      const userData = await authService.getMe();
      setUser(userData);
    } catch (err) {
      authService.clearTokens();
      const msg = formatErrorMessage(err);
      setError(msg);
      throw err;
    } finally {
      setIsLoading(false);
    }
  };

  const logout = async () => {
    setIsLoading(true);
    try {
      await authService.logout();
    } finally {
      realtimeService.disconnect();
      setUser(null);
      setIsLoading(false);
    }
  };

  const updateUser = (updatedUser: User) => {
    setUser(updatedUser);
  };

  return (
    <AuthContext.Provider
      value={{
        user,
        isAuthenticated: !!user,
        isLoading,
        error,
        login,
        register,
        logout,
        updateUser,
        refreshUser,
        clearError,
      }}
    >
      {children}
    </AuthContext.Provider>
  );
};
