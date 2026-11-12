export type SubscriptionPlan = 'FREE' | 'PREMIUM';

export type SubscriptionStatus = 'PENDING' | 'ACTIVE' | 'CANCELED' | 'EXPIRED' | 'PAST_DUE';

export type SubscriptionProvider = 'STRIPE' | 'MERCADOPAGO' | 'MOCK';

export interface Subscription {
  id?: string | null;
  userId: string;
  provider: SubscriptionProvider;
  plan: SubscriptionPlan;
  status: SubscriptionStatus;
  active: boolean;
  startedAt: string;
  expiresAt?: string | null;
  createdAt: string;
}

export interface InitiateSubscriptionData {
  plan?: SubscriptionPlan;
  returnUrl?: string;
}

export interface InitiateSubscriptionResponse {
  subscriptionId: string;
  sessionId: string;
  checkoutUrl: string;
  plan: SubscriptionPlan;
}
