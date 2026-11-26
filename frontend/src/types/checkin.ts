export interface Checkin {
  id: string;
  userId: string;
  communityId: string;
  caption?: string;
  mediaKeys?: string[];
  mediaUrls?: string[];
  commentsCount?: number;
  likesCount?: number;
  likedByCurrentUser?: boolean;
  createdAt: string;
  updatedAt: string;
}

export interface CreateCheckinData {
  communityId: string;
  caption?: string;
  mediaKeys?: string[];
}
