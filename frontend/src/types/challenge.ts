export type ChallengeScope = 'INDIVIDUAL' | 'COMMUNITY';
export type ChallengeMetric = 'CHECKINS' | 'STREAK';
export type ChallengeStatus = 'DRAFT' | 'ACTIVE' | 'COMPLETED' | 'CANCELLED';
export type RewardType = 'POINTS' | 'BADGE';

export interface ChallengeReward {
  id: string;
  type: RewardType;
  value?: number | null;
  badgeId?: string | null;
}

export interface Challenge {
  id: string;
  creatorId: string;
  communityId?: string | null;
  title: string;
  description?: string | null;
  scope: ChallengeScope;
  metric: ChallengeMetric;
  targetValue: number;
  startAt: string;
  endAt: string;
  status: ChallengeStatus;
  rewards: ChallengeReward[];
  createdAt: string;
}

export interface CreateChallengeData {
  title: string;
  description?: string;
  scope?: ChallengeScope;
  metric?: ChallengeMetric;
  targetValue: number;
  startAt?: string;
  endAt?: string;
  pointsReward?: number;
  badgeReward?: string;
}

export interface ChallengeParticipant {
  challengeId: string;
  userId: string;
  progress: number;
  targetValue: number;
  percentage: number;
  completed: boolean;
  completedAt?: string | null;
  joinedAt: string;
}
