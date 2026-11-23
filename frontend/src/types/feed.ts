export interface FeedItem {
  id: string;
  userId: string;
  authorUsername?: string;
  authorName?: string;
  authorAvatarUrl?: string | null;
  authorLevel?: number;
  authorTitle?: string;
  authorAvatarBorder?: string;
  communityId: string;
  communityName?: string;
  caption: string;
  mediaKeys: string[];
  commentsCount?: number;
  createdAt: string;
}

export interface FeedResponse {
  content: FeedItem[];
  nextCursor?: string | null;
  hasMore: boolean;
}
