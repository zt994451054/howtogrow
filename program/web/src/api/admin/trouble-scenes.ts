import { http } from "@/api/http";
import type { ApiResponse, PageResponse } from "@/api/types";

export type TroubleSceneView = {
  id: number;
  name: string;
  logoUrl: string | null;
  minAge: number;
  maxAge: number;
  status: number;
  createdAt: string;
  updatedAt: string;
};

export type TroubleSceneUpsertRequest = {
  name: string;
  logoUrl: string | null;
  minAge: number;
  maxAge: number;
};

export async function listTroubleScenes(params: {
  page: number;
  pageSize: number;
  keyword?: string;
  ageYear?: number;
}): Promise<PageResponse<TroubleSceneView>> {
  const res = await http.get<ApiResponse<PageResponse<TroubleSceneView>>>("/api/v1/admin/trouble-scenes", { params });
  return res.data.data;
}

export async function createTroubleScene(request: TroubleSceneUpsertRequest): Promise<number> {
  const res = await http.post<ApiResponse<number>>("/api/v1/admin/trouble-scenes", request);
  return res.data.data;
}

export async function updateTroubleScene(id: number, request: TroubleSceneUpsertRequest): Promise<void> {
  await http.put<ApiResponse<unknown>>(`/api/v1/admin/trouble-scenes/${id}`, request);
}

export async function deleteTroubleScene(id: number): Promise<void> {
  await http.delete<ApiResponse<unknown>>(`/api/v1/admin/trouble-scenes/${id}`);
}

export async function importTroubleScenesExcel(file: File): Promise<{ imported: number }> {
  const form = new FormData();
  form.append("file", file);
  const res = await http.post<ApiResponse<{ imported: number }>>("/api/v1/admin/trouble-scenes/import-excel", form, {
    headers: { "Content-Type": "multipart/form-data" }
  });
  return res.data.data;
}

export async function listAllTroubleScenes(pageSize = 200): Promise<TroubleSceneView[]> {
  const first = await listTroubleScenes({ page: 1, pageSize });
  const out = [...first.items];
  const totalPages = Math.ceil(first.total / pageSize);
  for (let page = 2; page <= totalPages; page += 1) {
    const res = await listTroubleScenes({ page, pageSize });
    out.push(...res.items);
  }
  return out;
}
