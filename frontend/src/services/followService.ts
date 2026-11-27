import { api } from './api';
import { FollowRequestItem, FollowStatusResponse } from '../types/auth';

export const followService = {
  followUser: async (targetUserId: string): Promise<FollowStatusResponse> => {
    const response = await api.post<FollowStatusResponse>(`/follows/${targetUserId}`);
    return response.data;
  },

  unfollowUser: async (targetUserId: string): Promise<FollowStatusResponse> => {
    const response = await api.delete<FollowStatusResponse>(`/follows/${targetUserId}`);
    return response.data;
  },

  getFollowStatus: async (targetUserId: string): Promise<FollowStatusResponse> => {
    const response = await api.get<FollowStatusResponse>(`/follows/${targetUserId}/status`);
    return response.data;
  },

  getPendingRequests: async (): Promise<FollowRequestItem[]> => {
    const response = await api.get<FollowRequestItem[]>('/follows/requests/pending');
    return response.data;
  },

  acceptRequest: async (requestId: string): Promise<void> => {
    await api.post(`/follows/requests/${requestId}/accept`);
  },

  rejectRequest: async (requestId: string): Promise<void> => {
    await api.post(`/follows/requests/${requestId}/reject`);
  },
};
