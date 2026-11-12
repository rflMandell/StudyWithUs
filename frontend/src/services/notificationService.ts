import { api } from './api';
import {
  InAppNotification,
  NotificationPreferences,
  PagedNotificationsResponse,
  RegisterDeviceData,
  UnreadCountResponse,
  UpdateNotificationPreferencesData,
} from '../types/notification';

export const notificationService = {
  async listNotifications(
    unreadOnly = false,
    page = 0,
    size = 20
  ): Promise<PagedNotificationsResponse> {
    const response = await api.get<PagedNotificationsResponse>('/notifications', {
      params: { unreadOnly, page, size },
    });
    return response.data;
  },

  async markAsRead(notificationId: string): Promise<InAppNotification> {
    const response = await api.patch<InAppNotification>(`/notifications/${notificationId}/read`);
    return response.data;
  },

  async markAllAsRead(): Promise<void> {
    await api.post('/notifications/read-all');
  },

  async getUnreadCount(): Promise<number> {
    const response = await api.get<UnreadCountResponse>('/notifications/unread-count');
    return response.data.unreadCount;
  },

  async getPreferences(): Promise<NotificationPreferences> {
    const response = await api.get<NotificationPreferences>('/notifications/preferences');
    return response.data;
  },

  async updatePreferences(
    data: UpdateNotificationPreferencesData
  ): Promise<NotificationPreferences> {
    const response = await api.put<NotificationPreferences>('/notifications/preferences', data);
    return response.data;
  },

  async registerDevice(data: RegisterDeviceData): Promise<void> {
    await api.post('/devices', data);
  },
};
