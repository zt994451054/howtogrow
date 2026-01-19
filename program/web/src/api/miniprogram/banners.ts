import { publicHttp } from "@/api/publicHttp";
import type { ApiResponse } from "@/api/types";

export type MiniprogramBannerDetailView = {
  id: number;
  title: string;
  htmlContent: string;
};

export async function getMiniprogramBannerDetail(id: number): Promise<MiniprogramBannerDetailView> {
  const res = await publicHttp.get<ApiResponse<MiniprogramBannerDetailView>>(`/api/v1/miniprogram/banners/${id}`);
  return res.data.data;
}

