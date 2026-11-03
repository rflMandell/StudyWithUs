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
  status: 'ACTIVE' | 'SUSPENDED' | 'DELETED';
  lifetimePoints: number;
  currentPoints?: number;
  activeCommunitiesCount?: number;
  checkinsCount?: number;
  currentStreak?: number;
  longestStreak?: number;
  badges?: UserBadge[];
  createdAt: string;
}

export interface UpdateUserData {
  name?: string;
  bio?: string;
  institution?: string;
  course?: string;
  studyGoal?: string;
  avatarUrl?: string;
  birthDate?: string;
}

export interface LoginCredentials {
  usernameOrEmail: string;
  password: string;
  deviceId?: string;
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
}

export interface ApiError {
  timestamp: string;
  status: number;
  code: string;
  message: string;
  path: string;
  traceId: string;
}
