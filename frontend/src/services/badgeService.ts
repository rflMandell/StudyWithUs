import { api } from './api';

export interface Badge {
  id: string;
  name: string;
  description: string;
  imageUrl?: string | null;
  createdAt: string;
}

export interface UserBadge {
  userId: string;
  badgeId: string;
  name: string;
  description: string;
  imageUrl?: string | null;
  awardedAt: string;
  challengeId?: string | null;
  communityId?: string | null;
}

export const badgeService = {
  async listAvailableBadges(): Promise<Badge[]> {
    const response = await api.get<Badge[]>('/badges');
    return response.data;
  },

  async getUserBadges(userId: string): Promise<UserBadge[]> {
    const response = await api.get<UserBadge[]>(`/users/${userId}/badges`);
    return response.data;
  },
};
