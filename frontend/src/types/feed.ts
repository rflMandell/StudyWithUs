export interface FeedItem {
  id: string;
  userId: string;
  authorUsername?: string;
  authorName?: string;
  authorAvatarUrl?: string | null;
  communityId: string;
  communityName?: string;
  caption: string;
  mediaKeys: string[];
  createdAt: string;
}

export interface FeedResponse {
  content: FeedItem[];
  nextCursor?: string | null;
  hasMore: boolean;
}
