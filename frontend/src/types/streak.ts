export interface CommunityStreak {
  communityId: string;
  userId: string;
  currentStreak: number;
  longestStreak: number;
  lastActivityDate?: string | null;
}
