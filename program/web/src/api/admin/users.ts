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

export async function listUsers(params: { page: number; pageSize: number }): Promise<PageResponse<UserView>> {
  const res = await http.get<ApiResponse<PageResponse<UserView>>>("/api/v1/admin/users", { params });
  return res.data.data;
}

