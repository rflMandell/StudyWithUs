import { api } from './api';
import { CommunityStreak } from '../types/streak';

export const streakService = {
  getUserStreak: async (): Promise<{ currentStreak: number; longestStreak: number; lastActivityDate?: string }> => {
    const response = await api.get('/users/me/streak');
    return response.data;
  },

  getCommunityStreak: async (communityId: string): Promise<CommunityStreak> => {
    const response = await api.get<CommunityStreak>(`/communities/${communityId}/streaks/me`);
    return response.data;
  },

  getCommunityFreeDays: async (communityId: string): Promise<number[]> => {
    const response = await api.get<number[]>(`/communities/${communityId}/free-days`);
    return response.data;
  },

  addCommunityFreeDay: async (communityId: string, dayOfWeek: number): Promise<void> => {
    await api.post(`/communities/${communityId}/free-days/${dayOfWeek}`);
  },

  removeCommunityFreeDay: async (communityId: string, dayOfWeek: number): Promise<void> => {
    await api.delete(`/communities/${communityId}/free-days/${dayOfWeek}`);
  },
};
