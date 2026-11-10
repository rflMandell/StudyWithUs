import { describe, it, expect, vi, beforeEach } from 'vitest';
import { badgeService } from '../services/badgeService';
import { api } from '../services/api';

vi.mock('../services/api');

describe('badgeService', () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  it('fetches list of available badges', async () => {
    const mockBadges = [
      {
        id: 'b1',
        name: 'Primeiro Check-in',
        description: 'Primeiro check-in de estudos',
        imageUrl: '/badges/first.png',
        createdAt: '2026-11-10T00:00:00Z',
      },
      {
        id: 'b2',
        name: '7 Dias Seguidos',
        description: 'Consistência por 7 dias',
        imageUrl: '/badges/streak7.png',
        createdAt: '2026-11-10T00:00:00Z',
      },
    ];

    vi.mocked(api.get).mockResolvedValue({ data: mockBadges });

    const result = await badgeService.listAvailableBadges();

    expect(api.get).toHaveBeenCalledWith('/badges');
    expect(result).toHaveLength(2);
    expect(result[0].name).toBe('Primeiro Check-in');
  });

  it('fetches user earned badges', async () => {
    const mockUserBadges = [
      {
        userId: 'user-1',
        badgeId: 'b1',
        name: 'Primeiro Check-in',
        description: 'Primeiro check-in de estudos',
        awardedAt: '2026-11-10T01:00:00Z',
      },
    ];

    vi.mocked(api.get).mockResolvedValue({ data: mockUserBadges });

    const result = await badgeService.getUserBadges('user-1');

    expect(api.get).toHaveBeenCalledWith('/users/user-1/badges');
    expect(result).toHaveLength(1);
    expect(result[0].badgeId).toBe('b1');
  });
});
