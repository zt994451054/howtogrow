import { http } from "@/api/http";
import type { ApiResponse } from "@/api/types";

export type DimensionView = {
  code: string;
  name: string;
  sortNo: number;
};

export async function listDimensions(): Promise<DimensionView[]> {
  const res = await http.get<ApiResponse<DimensionView[]>>("/api/v1/admin/dimensions");
  return res.data.data;
}
