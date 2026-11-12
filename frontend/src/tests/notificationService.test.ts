import { describe, it, expect, vi, beforeEach } from 'vitest';
import { notificationService } from '../services/notificationService';
import { api } from '../services/api';

vi.mock('../services/api');

describe('notificationService', () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  it('lists notifications with pagination and unreadOnly filter', async () => {
    const mockResponse = {
      content: [
        {
          id: 'notif-1',
          userId: 'u-1',
          type: 'NEW_COMMENT' as const,
          title: 'Novo comentário',
          body: 'Comentaram no seu check-in',
          read: false,
          createdAt: '2026-11-11T12:00:00Z',
        },
      ],
      page: 0,
      size: 20,
      totalElements: 1,
      totalPages: 1,
      hasNext: false,
    };

    vi.mocked(api.get).mockResolvedValue({ data: mockResponse });

    const result = await notificationService.listNotifications(true, 0, 20);

    expect(api.get).toHaveBeenCalledWith('/notifications', {
      params: { unreadOnly: true, page: 0, size: 20 },
    });
    expect(result.content).toHaveLength(1);
    expect(result.content[0].type).toBe('NEW_COMMENT');
  });

  it('marks a notification as read', async () => {
    const mockUpdated = {
      id: 'notif-1',
      userId: 'u-1',
      type: 'NEW_COMMENT' as const,
      title: 'Novo comentário',
      body: 'Comentaram no seu check-in',
      read: true,
      readAt: '2026-11-11T12:05:00Z',
      createdAt: '2026-11-11T12:00:00Z',
    };

    vi.mocked(api.patch).mockResolvedValue({ data: mockUpdated });

    const result = await notificationService.markAsRead('notif-1');

    expect(api.patch).toHaveBeenCalledWith('/notifications/notif-1/read');
    expect(result.read).toBe(true);
    expect(result.readAt).toBe('2026-11-11T12:05:00Z');
  });

  it('marks all notifications as read', async () => {
    vi.mocked(api.post).mockResolvedValue({});

    await notificationService.markAllAsRead();

    expect(api.post).toHaveBeenCalledWith('/notifications/read-all');
  });

  it('gets unread notifications count', async () => {
    vi.mocked(api.get).mockResolvedValue({ data: { unreadCount: 5 } });

    const count = await notificationService.getUnreadCount();

    expect(api.get).toHaveBeenCalledWith('/notifications/unread-count');
    expect(count).toBe(5);
  });

  it('gets and updates notification preferences', async () => {
    const mockPrefs = {
      userId: 'u-1',
      commentsEnabled: true,
      rankingEnabled: true,
      challengeEnabled: true,
      invitationEnabled: true,
      reminderEnabled: true,
      streakEnabled: true,
      updatedAt: '2026-11-11T12:00:00Z',
    };

    vi.mocked(api.get).mockResolvedValue({ data: mockPrefs });

    const prefs = await notificationService.getPreferences();
    expect(api.get).toHaveBeenCalledWith('/notifications/preferences');
    expect(prefs.commentsEnabled).toBe(true);

    const updatedPrefs = { ...mockPrefs, commentsEnabled: false };
    vi.mocked(api.put).mockResolvedValue({ data: updatedPrefs });

    const result = await notificationService.updatePreferences({ commentsEnabled: false });
    expect(api.put).toHaveBeenCalledWith('/notifications/preferences', { commentsEnabled: false });
    expect(result.commentsEnabled).toBe(false);
  });

  it('registers a device token', async () => {
    vi.mocked(api.post).mockResolvedValue({});

    await notificationService.registerDevice({ token: 'fcm-token-123', platform: 'ANDROID' });

    expect(api.post).toHaveBeenCalledWith('/devices', {
      token: 'fcm-token-123',
      platform: 'ANDROID',
    });
  });
});
