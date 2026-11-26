export interface CommentAuthor {
  id: string;
  username: string;
  name: string;
  avatarUrl?: string | null;
}

export interface Comment {
  id: string;
  checkinId: string;
  parentCommentId?: string | null;
  author: CommentAuthor;
  content: string;
  likesCount?: number;
  likedByCurrentUser?: boolean;
  createdAt: string;
  updatedAt: string;
}

export interface CreateCommentData {
  content: string;
  parentCommentId?: string | null;
}

export interface LikeToggleResponse {
  targetId: string;
  liked: boolean;
  likesCount: number;
}

export interface PagedCommentsResponse {
  content: Comment[];
  page: number;
  size: number;
  totalElements: number;
  totalPages: number;
  hasNext: boolean;
}
