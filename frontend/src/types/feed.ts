export interface FeedItem {
  id: string;
  userId: string;
  authorUsername?: string;
  authorName?: string;
  authorAvatarUrl?: string | null;
  authorLevel?: number;
  authorTitle?: string;
  authorAvatarBorder?: string;
  communityId?: string | null;
  communityName?: string | null;
  caption: string;
  mediaKeys: string[];
  mediaUrls?: string[];
  commentsCount?: number;
  likesCount?: number;
  likedByCurrentUser?: boolean;
  countsAsCheckin?: boolean;
  createdAt: string;
}

export interface FeedResponse {
  content: FeedItem[];
  nextCursor?: string | null;
  hasMore: boolean;
}
