export type RealtimeNotificationType =
  | 'NEW_CHECKIN'
  | 'RANKING_UPDATED'
  | 'STREAK_UPDATED'
  | 'POINTS_AWARDED'
  | 'MEMBER_JOINED';

export interface RealtimeNotification {
  id: string;
  type: RealtimeNotificationType;
  recipientUserId?: string | null;
  communityId?: string | null;
  title: string;
  message: string;
  data: Record<string, any>;
  timestamp: string;
}
