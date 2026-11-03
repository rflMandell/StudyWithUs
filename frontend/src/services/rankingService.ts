import { api } from './api';
import { RankingEntry, UserRankingPosition } from '../types/ranking';
import { PagedResponse } from '../types/community';

export const rankingService = {
  async getCommunityRanking(communityId: string, page = 0, size = 20): Promise<PagedResponse<RankingEntry>> {
    const response = await api.get<PagedResponse<RankingEntry>>(`/communities/${communityId}/ranking`, {
      params: { page, size },
    });
    return response.data;
  },

  async getMyRankingPosition(communityId: string): Promise<UserRankingPosition> {
    const response = await api.get<UserRankingPosition>(`/communities/${communityId}/ranking/me`);
    return response.data;
  },
};
