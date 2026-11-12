export type NotificationType =
  | 'NEW_CHECKIN'
  | 'RANKING_UPDATED'
  | 'STREAK_UPDATED'
  | 'POINTS_AWARDED'
  | 'MEMBER_JOINED'
  | 'NEW_COMMENT'
  | 'BADGE_GRANTED'
  | 'CHALLENGE_COMPLETED'
  | 'JOIN_REQUEST_APPROVED';

export interface InAppNotification {
  id: string;
  userId: string;
  type: NotificationType;
  title: string;
  body: string;
  referenceType?: string | null;
  referenceId?: string | null;
  read: boolean;
  readAt?: string | null;
  createdAt: string;
}

export interface PagedNotificationsResponse {
  content: InAppNotification[];
  page: number;
  size: number;
  totalElements: number;
  totalPages: number;
  hasNext: boolean;
}

export interface NotificationPreferences {
  userId: string;
  commentsEnabled: boolean;
  rankingEnabled: boolean;
  challengeEnabled: boolean;
  invitationEnabled: boolean;
  reminderEnabled: boolean;
  streakEnabled: boolean;
  updatedAt: string;
}

export interface UpdateNotificationPreferencesData {
  commentsEnabled?: boolean;
  rankingEnabled?: boolean;
  challengeEnabled?: boolean;
  invitationEnabled?: boolean;
  reminderEnabled?: boolean;
  streakEnabled?: boolean;
}

export interface RegisterDeviceData {
  token: string;
  platform?: string;
}

export interface UnreadCountResponse {
  unreadCount: number;
}
