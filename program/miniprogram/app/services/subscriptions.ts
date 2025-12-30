import { apiRequest } from "./request";
import type { SubscriptionOrderCreateRequest, SubscriptionOrderCreateResponse, SubscriptionPlanView } from "./types";

export async function fetchPlans(): Promise<SubscriptionPlanView[]> {
  return apiRequest<SubscriptionPlanView[]>("GET", "/miniprogram/subscriptions/plans");
}

export async function createOrder(payload: SubscriptionOrderCreateRequest): Promise<SubscriptionOrderCreateResponse> {
  return apiRequest<SubscriptionOrderCreateResponse>("POST", "/miniprogram/subscriptions/orders", payload);
}

