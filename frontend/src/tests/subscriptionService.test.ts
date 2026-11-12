import { describe, it, expect, vi, beforeEach } from 'vitest';
import { subscriptionService } from '../services/subscriptionService';
import { api } from '../services/api';

vi.mock('../services/api');

describe('subscriptionService', () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  it('gets current subscription', async () => {
    const mockSubscription = {
      id: 'sub-1',
      userId: 'u-1',
      provider: 'STRIPE' as const,
      plan: 'PREMIUM' as const,
      status: 'ACTIVE' as const,
      active: true,
      startedAt: '2026-11-12T10:00:00Z',
      expiresAt: '2026-12-12T10:00:00Z',
      createdAt: '2026-11-12T10:00:00Z',
    };

    vi.mocked(api.get).mockResolvedValue({ data: mockSubscription });

    const result = await subscriptionService.getSubscription();

    expect(api.get).toHaveBeenCalledWith('/subscription');
    expect(result.plan).toBe('PREMIUM');
    expect(result.active).toBe(true);
  });

  it('initiates subscription checkout', async () => {
    const mockResponse = {
      subscriptionId: 'sub-1',
      sessionId: 'cs_123',
      checkoutUrl: 'https://checkout.studywithus.com/cs_123',
      plan: 'PREMIUM' as const,
    };

    vi.mocked(api.post).mockResolvedValue({ data: mockResponse });

    const result = await subscriptionService.initiateSubscription({
      plan: 'PREMIUM',
      returnUrl: 'https://app.studywithus.com/success',
    });

    expect(api.post).toHaveBeenCalledWith('/subscription', {
      plan: 'PREMIUM',
      returnUrl: 'https://app.studywithus.com/success',
    });
    expect(result.sessionId).toBe('cs_123');
    expect(result.checkoutUrl).toBe('https://checkout.studywithus.com/cs_123');
  });

  it('cancels active subscription', async () => {
    const mockCanceled = {
      id: 'sub-1',
      userId: 'u-1',
      provider: 'STRIPE' as const,
      plan: 'PREMIUM' as const,
      status: 'CANCELED' as const,
      active: false,
      startedAt: '2026-11-12T10:00:00Z',
      expiresAt: '2026-12-12T10:00:00Z',
      createdAt: '2026-11-12T10:00:00Z',
    };

    vi.mocked(api.post).mockResolvedValue({ data: mockCanceled });

    const result = await subscriptionService.cancelSubscription();

    expect(api.post).toHaveBeenCalledWith('/subscription/cancel');
    expect(result.status).toBe('CANCELED');
  });
});
