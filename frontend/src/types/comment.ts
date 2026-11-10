export interface CommentAuthor {
  id: string;
  username: string;
  name: string;
  avatarUrl?: string | null;
}

export interface Comment {
  id: string;
  checkinId: string;
  author: CommentAuthor;
  content: string;
  createdAt: string;
  updatedAt: string;
}

export interface CreateCommentData {
  content: string;
}

export interface PagedCommentsResponse {
  content: Comment[];
  page: number;
  size: number;
  totalElements: number;
  totalPages: number;
  hasNext: boolean;
}
