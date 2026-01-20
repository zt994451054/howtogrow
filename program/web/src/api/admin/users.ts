import { http } from "@/api/http";
import type { ApiResponse, PageResponse } from "@/api/types";

export type UserView = {
  userId: number;
  wechatOpenid: string | null;
  nickname: string | null;
  avatarUrl: string | null;
  subscriptionEndAt: string | null;
  freeTrialUsed: boolean;
  createdAt: string;
};

export type UserListParams = {
  page: number;
  pageSize: number;
  userId?: number;
  keyword?: string;
  freeTrialUsed?: boolean;
  subscriptionStatus?: "ACTIVE" | "EXPIRED" | "NONE";
};

export async function listUsers(params: UserListParams): Promise<PageResponse<UserView>> {
  const res = await http.get<ApiResponse<PageResponse<UserView>>>("/api/v1/admin/users", { params });
  return res.data.data;
}

export async function extendUserSubscription(
  userId: number,
  request: { days: number }
): Promise<{ userId: number; subscriptionEndAt: string | null }> {
  const res = await http.post<ApiResponse<{ userId: number; subscriptionEndAt: string | null }>>(
    `/api/v1/admin/users/${userId}/subscription/extend`,
    request
  );
  return res.data.data;
}
