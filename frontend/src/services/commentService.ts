import { api } from './api';
import { Comment, CreateCommentData, LikeToggleResponse, PagedCommentsResponse } from '../types/comment';

export const commentService = {
  async listComments(checkinId: string, page = 0, size = 20): Promise<PagedCommentsResponse> {
    const response = await api.get<PagedCommentsResponse>(`/checkins/${checkinId}/comments`, {
      params: { page, size },
    });
    return response.data;
  },

  async createComment(checkinId: string, data: CreateCommentData): Promise<Comment> {
    const response = await api.post<Comment>(`/checkins/${checkinId}/comments`, data);
    return response.data;
  },

  async toggleLike(commentId: string): Promise<LikeToggleResponse> {
    const response = await api.post<LikeToggleResponse>(`/comments/${commentId}/like`);
    return response.data;
  },

  async deleteComment(commentId: string): Promise<void> {
    await api.delete(`/comments/${commentId}`);
  },
};
