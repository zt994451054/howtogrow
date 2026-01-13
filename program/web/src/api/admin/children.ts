import { http } from "@/api/http";
import type { ApiResponse, PageResponse } from "@/api/types";

export type AdminChildView = {
  childId: number;
  userId: number;
  userNickname: string | null;
  userAvatarUrl: string | null;
  childNickname: string;
  gender: number;
  birthDate: string;
  status: number;
  createdAt: string;
};

export async function listChildren(params: {
  page: number;
  pageSize: number;
  userId?: number;
  userNickname?: string;
  childId?: number;
  childNickname?: string;
  gender?: number;
  status?: number;
}): Promise<PageResponse<AdminChildView>> {
  const res = await http.get<ApiResponse<PageResponse<AdminChildView>>>("/api/v1/admin/children", { params });
  return res.data.data;
}

