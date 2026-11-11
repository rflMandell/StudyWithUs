import { api } from './api';
import { Challenge, ChallengeParticipant, CreateChallengeData } from '../types/challenge';

export const challengeService = {
  async createCommunityChallenge(communityId: string, data: CreateChallengeData): Promise<Challenge> {
    const response = await api.post<Challenge>(`/communities/${communityId}/challenges`, data);
    return response.data;
  },

  async listCommunityChallenges(communityId: string): Promise<Challenge[]> {
    const response = await api.get<Challenge[]>(`/communities/${communityId}/challenges`);
    return response.data;
  },

  async joinChallenge(challengeId: string): Promise<ChallengeParticipant> {
    const response = await api.post<ChallengeParticipant>(`/challenges/${challengeId}/join`);
    return response.data;
  },

  async getMyProgress(challengeId: string): Promise<ChallengeParticipant> {
    const response = await api.get<ChallengeParticipant>(`/challenges/${challengeId}/progress/me`);
    return response.data;
  },
};
