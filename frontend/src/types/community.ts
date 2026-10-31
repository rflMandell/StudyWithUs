export type CommunityVisibility = 'PUBLIC' | 'PRIVATE';
export type CommunityRole = 'OWNER' | 'ADMIN' | 'MODERATOR' | 'MEMBER';
export type MemberStatus = 'ACTIVE' | 'LEFT' | 'BANNED';
export type JoinRequestStatus = 'PENDING' | 'APPROVED' | 'REJECTED' | 'CANCELLED';

export interface Community {
  id: string;
  name: string;
  description?: string;
  imageUrl?: string;
  visibility: CommunityVisibility;
  ownerId: string;
  rankingType: string;
  timezone: string;
  membersCount: number;
  currentUserRole?: CommunityRole | null;
  createdAt: string;
}

export interface CommunityMember {
  communityId: string;
  userId: string;
  username: string;
  name: string;
  avatarUrl?: string;
  role: CommunityRole;
  status: MemberStatus;
  joinedAt: string;
}

export interface CommunityJoinRequest {
  id: string;
  communityId: string;
  userId: string;
  status: JoinRequestStatus;
  createdAt: string;
  reviewedAt?: string;
  reviewedBy?: string;
}

export interface CreateCommunityData {
  name: string;
  description?: string;
  imageUrl?: string;
  visibility: CommunityVisibility;
  timezone?: string;
}

export interface PagedResponse<T> {
  content: T[];
  page: number;
  size: number;
  totalElements: number;
  totalPages: number;
  hasNext: boolean;
}
