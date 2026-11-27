import { api } from './api';

export interface NotificationPreferences {
  commentsEnabled: boolean;
  rankingEnabled: boolean;
  challengeEnabled: boolean;
  invitationEnabled: boolean;
  reminderEnabled: boolean;
  streakEnabled: boolean;
  updatedAt?: string;
}

export const notificationService = {
  getPreferences: async (): Promise<NotificationPreferences> => {
    const response = await api.get<NotificationPreferences>('/notifications/preferences');
    return response.data;
  },

  updatePreferences: async (
    prefs: Omit<NotificationPreferences, 'updatedAt'>
  ): Promise<NotificationPreferences> => {
    const response = await api.put<NotificationPreferences>('/notifications/preferences', prefs);
    return response.data;
  },
};
