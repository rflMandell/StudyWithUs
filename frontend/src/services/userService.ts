import { api } from './api';
import { User, UpdateUserData, UserBadge } from '../types/auth';

export const userService = {
  getProfile: async (userId?: string): Promise<User> => {
    const endpoint = userId ? `/users/${userId}` : '/users/me';
    const response = await api.get<User>(endpoint);
    return response.data;
  },

  updateProfile: async (data: UpdateUserData): Promise<User> => {
    const response = await api.patch<User>('/users/me', data);
    return response.data;
  },

  getUserStreak: async (): Promise<{ currentStreak: number; longestStreak: number; lastActivityDate?: string }> => {
    const response = await api.get<{ currentStreak: number; longestStreak: number; lastActivityDate?: string }>('/users/me/streak');
    return response.data;
  },

  getUserBadges: async (userId: string): Promise<UserBadge[]> => {
    const response = await api.get<UserBadge[]>(`/users/${userId}/badges`);
    return response.data;
  },
};
