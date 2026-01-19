import { http } from "@/api/http";
import type { ApiResponse, PageResponse } from "@/api/types";

export type QuoteView = {
  id: number;
  content: string;
  scene: string;
  minAge: number;
  maxAge: number;
  status: number;
};

export type QuoteUpsertRequest = {
  content: string;
  scene: string;
  minAge: number;
  maxAge: number;
  status: number;
};

export async function listQuotes(params: {
  page: number;
  pageSize: number;
  scene?: string;
  status?: number;
  keyword?: string;
}): Promise<PageResponse<QuoteView>> {
  const res = await http.get<ApiResponse<PageResponse<QuoteView>>>("/api/v1/admin/quotes", { params });
  return res.data.data;
}

export async function createQuote(request: QuoteUpsertRequest): Promise<void> {
  await http.post<ApiResponse<unknown>>("/api/v1/admin/quotes", request);
}

export async function updateQuote(id: number, request: QuoteUpsertRequest): Promise<void> {
  await http.put<ApiResponse<unknown>>(`/api/v1/admin/quotes/${id}`, request);
}

export async function deleteQuote(id: number): Promise<void> {
  await http.delete<ApiResponse<unknown>>(`/api/v1/admin/quotes/${id}`);
}
