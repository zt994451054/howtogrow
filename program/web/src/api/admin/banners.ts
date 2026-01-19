import { http } from "@/api/http";
import type { ApiResponse, PageResponse } from "@/api/types";

export type BannerView = {
  id: number;
  title: string;
  imageUrl: string;
  htmlContent: string;
  status: number;
  sortNo: number;
  createdAt: string;
  updatedAt: string;
};

export type BannerUpsertRequest = {
  title: string;
  imageUrl: string;
  htmlContent: string;
  status: number;
  sortNo: number;
};

export async function listBanners(params: {
  page: number;
  pageSize: number;
  status?: number;
  keyword?: string;
}): Promise<PageResponse<BannerView>> {
  const res = await http.get<ApiResponse<PageResponse<BannerView>>>("/api/v1/admin/banners", { params });
  return res.data.data;
}

export async function createBanner(request: BannerUpsertRequest): Promise<number> {
  const res = await http.post<ApiResponse<number>>("/api/v1/admin/banners", request);
  return res.data.data;
}

export async function updateBanner(id: number, request: BannerUpsertRequest): Promise<void> {
  await http.put<ApiResponse<unknown>>(`/api/v1/admin/banners/${id}`, request);
}

export async function deleteBanner(id: number): Promise<void> {
  await http.delete<ApiResponse<unknown>>(`/api/v1/admin/banners/${id}`);
}
