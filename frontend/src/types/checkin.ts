export interface Checkin {
  id: string;
  userId: string;
  communityId?: string | null;
  caption?: string;
  mediaKeys?: string[];
  mediaUrls?: string[];
  commentsCount?: number;
  likesCount?: number;
  likedByCurrentUser?: boolean;
  countsAsCheckin?: boolean;
  createdAt: string;
  updatedAt: string;
}

export interface CreateCheckinData {
  communityId?: string | null;
  caption?: string;
  mediaKeys?: string[];
  countsAsCheckin?: boolean;
}
