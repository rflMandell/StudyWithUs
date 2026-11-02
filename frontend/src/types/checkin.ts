export interface Checkin {
  id: string;
  userId: string;
  communityId: string;
  caption?: string;
  mediaKeys?: string[];
  createdAt: string;
  updatedAt: string;
}

export interface CreateCheckinData {
  communityId: string;
  caption?: string;
  mediaKeys?: string[];
}
