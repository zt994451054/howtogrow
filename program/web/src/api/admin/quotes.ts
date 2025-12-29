import { http } from "@/api/http";
import type { ApiResponse } from "@/api/types";

export type QuoteView = {
  id: number;
  content: string;
  status: number;
};

export type QuoteUpsertRequest = {
  content: string;
  status: number;
};

export async function listQuotes(): Promise<QuoteView[]> {
  const res = await http.get<ApiResponse<QuoteView[]>>("/api/v1/admin/quotes");
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

