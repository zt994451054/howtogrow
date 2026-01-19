import { http } from "@/api/http";
import type { ApiResponse } from "@/api/types";

export async function uploadPublic(file: File): Promise<string> {
  const form = new FormData();
  form.append("file", file);
  const res = await http.post<ApiResponse<{ url: string }>>("/api/v1/admin/uploads/public", form, {
    headers: { "Content-Type": "multipart/form-data" }
  });
  return res.data.data.url;
}

