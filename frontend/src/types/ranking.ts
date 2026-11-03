export interface RankingEntry {
  rankingPeriodId: string;
  userId: string;
  username: string;
  name: string;
  avatarUrl?: string;
  points: number;
  position: number;
  streakAtUpdate: number;
  updatedAt: string;
}

export interface UserRankingPosition {
  communityId: string;
  rankingPeriodId: string;
  userId: string;
  points: number;
  position: number;
  streakAtUpdate: number;
}
