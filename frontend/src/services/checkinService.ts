import { api } from './api';
import { Checkin, CreateCheckinData } from '../types/checkin';
import { PagedResponse } from '../types/community';

export const checkinService = {
  async createCheckin(data: CreateCheckinData, idempotencyKey?: string): Promise<Checkin> {
    const headers: Record<string, string> = {};
    if (idempotencyKey) {
      headers['Idempotency-Key'] = idempotencyKey;
    }
    const response = await api.post<Checkin>('/checkins', data, { headers });
    return response.data;
  },

  async getCheckinDetails(checkinId: string): Promise<Checkin> {
    const response = await api.get<Checkin>(`/checkins/${checkinId}`);
    return response.data;
  },

  async listCommunityCheckins(communityId: string, page = 0, size = 20): Promise<PagedResponse<Checkin>> {
    const response = await api.get<PagedResponse<Checkin>>(`/communities/${communityId}/checkins`, {
      params: { page, size },
    });
    return response.data;
  },

  async deleteCheckin(checkinId: string): Promise<void> {
    await api.delete(`/checkins/${checkinId}`);
  },
};
