import { describe, it, expect, vi, beforeEach } from 'vitest';
import { challengeService } from '../services/challengeService';
import { api } from '../services/api';

vi.mock('../services/api');

describe('challengeService', () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  it('creates a community challenge', async () => {
    const mockChallenge = {
      id: 'c-1',
      creatorId: 'u-1',
      communityId: 'comm-1',
      title: 'Desafio 10 Check-ins',
      description: 'Complete 10 check-ins no mês',
      scope: 'INDIVIDUAL' as const,
      metric: 'CHECKINS' as const,
      targetValue: 10,
      startAt: '2026-11-01T00:00:00Z',
      endAt: '2026-11-30T23:59:59Z',
      status: 'ACTIVE' as const,
      rewards: [{ id: 'r-1', type: 'POINTS' as const, value: 50 }],
      createdAt: '2026-11-01T00:00:00Z',
    };

    vi.mocked(api.post).mockResolvedValue({ data: mockChallenge });

    const result = await challengeService.createCommunityChallenge('comm-1', {
      title: 'Desafio 10 Check-ins',
      targetValue: 10,
      pointsReward: 50,
    });

    expect(api.post).toHaveBeenCalledWith('/communities/comm-1/challenges', {
      title: 'Desafio 10 Check-ins',
      targetValue: 10,
      pointsReward: 50,
    });
    expect(result.id).toBe('c-1');
    expect(result.targetValue).toBe(10);
  });

  it('lists community challenges', async () => {
    const mockList = [
      {
        id: 'c-1',
        title: 'Desafio 10 Check-ins',
        targetValue: 10,
        rewards: [],
      },
    ];

    vi.mocked(api.get).mockResolvedValue({ data: mockList });

    const result = await challengeService.listCommunityChallenges('comm-1');

    expect(api.get).toHaveBeenCalledWith('/communities/comm-1/challenges');
    expect(result).toHaveLength(1);
    expect(result[0].title).toBe('Desafio 10 Check-ins');
  });

  it('joins a challenge', async () => {
    const mockParticipant = {
      challengeId: 'c-1',
      userId: 'u-2',
      progress: 0,
      targetValue: 10,
      percentage: 0,
      completed: false,
      joinedAt: '2026-11-10T12:00:00Z',
    };

    vi.mocked(api.post).mockResolvedValue({ data: mockParticipant });

    const result = await challengeService.joinChallenge('c-1');

    expect(api.post).toHaveBeenCalledWith('/challenges/c-1/join');
    expect(result.challengeId).toBe('c-1');
    expect(result.completed).toBe(false);
  });

  it('fetches user challenge progress', async () => {
    const mockProgress = {
      challengeId: 'c-1',
      userId: 'u-2',
      progress: 8,
      targetValue: 10,
      percentage: 80.0,
      completed: false,
      joinedAt: '2026-11-10T12:00:00Z',
    };

    vi.mocked(api.get).mockResolvedValue({ data: mockProgress });

    const result = await challengeService.getMyProgress('c-1');

    expect(api.get).toHaveBeenCalledWith('/challenges/c-1/progress/me');
    expect(result.progress).toBe(8);
    expect(result.percentage).toBe(80.0);
  });
});
