import { api } from './api';
import { FeedResponse } from '../types/feed';

export const feedService = {
  getFeed: async (cursor?: string | null, limit: number = 10): Promise<FeedResponse> => {
    const params = new URLSearchParams();
    if (cursor) {
      params.append('cursor', cursor);
    }
    params.append('limit', limit.toString());

    const response = await api.get<FeedResponse>(`/feed?${params.toString()}`);
    return response.data;
  },
};
