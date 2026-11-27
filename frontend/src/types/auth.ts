export type GlobalRole = 'USER' | 'SUPPORT' | 'ADMIN' | 'SUPER_ADMIN';

export interface UserBadge {
  badgeId: string;
  name: string;
  description: string;
  imageUrl?: string;
  awardedAt: string;
}

export interface User {
  id: string;
  username: string;
  name: string;
  email: string;
  birthDate?: string;
  avatarUrl?: string;
  institution?: string;
  course?: string;
  studyGoal?: string;
  bio?: string;
  status: 'ACTIVE' | 'SUSPENDED' | 'DEACTIVATED' | 'DELETED';
  isPrivate?: boolean;
  privacyUpdatedAt?: string;
  globalRole?: GlobalRole;
  lifetimePoints: number;
  currentPoints?: number;
  activeCommunitiesCount?: number;
  checkinsCount?: number;
  currentStreak?: number;
  longestStreak?: number;
  badges?: UserBadge[];
  level?: number;
  totalXp?: number;
  currentLevelXp?: number;
  nextLevelXp?: number;
  progressPercent?: number;
  activeTitle?: string;
  activeAvatarBorder?: string;
  createdAt: string;
}

export interface UpdateUserData {
  name?: string;
  bio?: string;
  institution?: string;
  course?: string;
  studyGoal?: string;
  birthDate?: string;
}

export interface LoginCredentials {
  usernameOrEmail: string;
  password: string;
  deviceId?: string;
  totpCode?: string;
}

export interface RegisterData {
  username: string;
  name: string;
  email: string;
  password: string;
  birthDate?: string;
  institution?: string;
  course?: string;
  studyGoal?: string;
  bio?: string;
  deviceId?: string;
}

export interface AuthResponse {
  accessToken: string;
  refreshToken: string;
  tokenType: string;
  expiresIn: number;
  userId: string;
  username: string;
  email: string;
  name: string;
  globalRole?: GlobalRole;
}

export interface ApiError {
  timestamp: string;
  status: number;
  code: string;
  message: string;
  path: string;
  traceId: string;
}

export interface UserSearchItem {
  id: string;
  username: string;
  name: string;
  avatarUrl?: string | null;
  isPrivate: boolean;
}

export type FollowStatus = 'SELF' | 'FOLLOWING' | 'REQUESTED' | 'NONE';

export interface FollowStatusResponse {
  status: FollowStatus;
  followersCount: number;
  followingCount: number;
}

export interface FollowRequestItem {
  id: string;
  requesterId: string;
  requesterUsername: string;
  requesterName: string;
  requesterAvatarUrl?: string | null;
  createdAt: string;
}
