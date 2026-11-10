export interface Checkin {
  id: string;
  userId: string;
  communityId: string;
  caption?: string;
  mediaKeys?: string[];
  commentsCount?: number;
  createdAt: string;
  updatedAt: string;
}

export interface CreateCheckinData {
  communityId: string;
  caption?: string;
  mediaKeys?: string[];
}
