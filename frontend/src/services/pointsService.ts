import { api } from './api';
import { PointTransaction, UserPointsSummary } from '../types/points';
import { PagedResponse } from '../types/community';

export const pointsService = {
  async getPointsSummary(): Promise<UserPointsSummary> {
    const response = await api.get<UserPointsSummary>('/users/me/points');
    return response.data;
  },

  async getPointsHistory(page = 0, size = 20): Promise<PagedResponse<PointTransaction>> {
    const response = await api.get<PagedResponse<PointTransaction>>('/users/me/points/history', {
      params: { page, size },
    });
    return response.data;
  },
};
