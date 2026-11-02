export type PointTransactionType = 'CHECKIN' | 'CHALLENGE' | 'BONUS' | 'REVERSAL' | 'ADMIN_ADJUSTMENT';

export interface UserPointsSummary {
  userId: string;
  currentPoints: number;
  lifetimePoints: number;
}

export interface PointTransaction {
  id: string;
  userId: string;
  communityId?: string;
  checkinId?: string;
  challengeId?: string;
  amount: number;
  type: PointTransactionType;
  createdAt: string;
  reversedAt?: string;
  reversalId?: string;
}
