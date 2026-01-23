import { http } from "@/api/http";
import type { ApiResponse } from "@/api/types";

export type PlanView = {
  planId: number;
  name: string;
  days: number;
  originalPriceCent: number;
  priceCent: number;
  status: number;
};

export type PlanUpsertRequest = {
  name: string;
  days: number;
  originalPriceCent: number;
  priceCent: number;
  status: number;
};

export async function listPlans(): Promise<PlanView[]> {
  const res = await http.get<ApiResponse<PlanView[]>>("/api/v1/admin/plans");
  return res.data.data;
}

export async function createPlan(request: PlanUpsertRequest): Promise<void> {
  await http.post<ApiResponse<unknown>>("/api/v1/admin/plans", request);
}

export async function updatePlan(planId: number, request: PlanUpsertRequest): Promise<void> {
  await http.put<ApiResponse<unknown>>(`/api/v1/admin/plans/${planId}`, request);
}

export async function deletePlan(planId: number): Promise<void> {
  await http.delete<ApiResponse<unknown>>(`/api/v1/admin/plans/${planId}`);
}
