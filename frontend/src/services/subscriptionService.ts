import { api } from './api';
import {
  InitiateSubscriptionData,
  InitiateSubscriptionResponse,
  Subscription,
} from '../types/subscription';

export const subscriptionService = {
  async getSubscription(): Promise<Subscription> {
    const response = await api.get<Subscription>('/subscription');
    return response.data;
  },

  async initiateSubscription(
    data?: InitiateSubscriptionData
  ): Promise<InitiateSubscriptionResponse> {
    const response = await api.post<InitiateSubscriptionResponse>(
      '/subscription',
      data || { plan: 'PREMIUM' }
    );
    return response.data;
  },

  async cancelSubscription(): Promise<Subscription> {
    const response = await api.post<Subscription>('/subscription/cancel');
    return response.data;
  },
};
