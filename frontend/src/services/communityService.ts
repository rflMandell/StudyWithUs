import { api } from './api';
import {
  Community,
  CommunityJoinRequest,
  CommunityMember,
  CommunityRole,
  CreateCommunityData,
  JoinRequestStatus,
  PagedResponse,
} from '../types/community';

export const communityService = {
  async listCommunities(search?: string, page = 0, size = 20): Promise<PagedResponse<Community>> {
    const params: Record<string, unknown> = { page, size };
    if (search && search.trim()) {
      params.search = search.trim();
    }
    const response = await api.get<PagedResponse<Community>>('/communities', { params });
    return response.data;
  },

  async getCommunityDetails(communityId: string): Promise<Community> {
    const response = await api.get<Community>(`/communities/${communityId}`);
    return response.data;
  },

  async createCommunity(data: CreateCommunityData): Promise<Community> {
    const response = await api.post<Community>('/communities', data);
    return response.data;
  },

  async joinCommunity(communityId: string): Promise<CommunityMember> {
    const response = await api.post<CommunityMember>(`/communities/${communityId}/members`);
    return response.data;
  },

  async leaveCommunity(communityId: string): Promise<void> {
    await api.delete(`/communities/${communityId}/members/me`);
  },

  async listMembers(communityId: string, page = 0, size = 20): Promise<PagedResponse<CommunityMember>> {
    const response = await api.get<PagedResponse<CommunityMember>>(`/communities/${communityId}/members`, {
      params: { page, size },
    });
    return response.data;
  },

  async requestJoin(communityId: string): Promise<CommunityJoinRequest> {
    const response = await api.post<CommunityJoinRequest>(`/communities/${communityId}/join-requests`);
    return response.data;
  },

  async listJoinRequests(
    communityId: string,
    status: JoinRequestStatus = 'PENDING',
    page = 0,
    size = 20
  ): Promise<PagedResponse<CommunityJoinRequest>> {
    const response = await api.get<PagedResponse<CommunityJoinRequest>>(`/communities/${communityId}/join-requests`, {
      params: { status, page, size },
    });
    return response.data;
  },

  async approveJoinRequest(communityId: string, requestId: string): Promise<CommunityJoinRequest> {
    const response = await api.post<CommunityJoinRequest>(
      `/communities/${communityId}/join-requests/${requestId}/approve`
    );
    return response.data;
  },

  async rejectJoinRequest(communityId: string, requestId: string): Promise<CommunityJoinRequest> {
    const response = await api.post<CommunityJoinRequest>(
      `/communities/${communityId}/join-requests/${requestId}/reject`
    );
    return response.data;
  },

  async updateMemberRole(communityId: string, userId: string, role: CommunityRole): Promise<CommunityMember> {
    const response = await api.put<CommunityMember>(`/communities/${communityId}/members/${userId}/role`, { role });
    return response.data;
  },

  async removeMember(communityId: string, userId: string): Promise<void> {
    await api.delete(`/communities/${communityId}/members/${userId}`);
  },
};
