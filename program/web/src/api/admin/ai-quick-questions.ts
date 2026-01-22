import { http } from "@/api/http";
import type { ApiResponse, PageResponse } from "@/api/types";

export type AiQuickQuestionView = {
  id: number;
  prompt: string;
  status: number;
  sortNo: number;
  createdAt: string;
  updatedAt: string;
};

export type AiQuickQuestionUpsertRequest = {
  prompt: string;
  status: number;
  sortNo: number;
};

export async function listAiQuickQuestions(params: {
  page: number;
  pageSize: number;
  status?: number;
  keyword?: string;
}): Promise<PageResponse<AiQuickQuestionView>> {
  const res = await http.get<ApiResponse<PageResponse<AiQuickQuestionView>>>("/api/v1/admin/ai-quick-questions", { params });
  return res.data.data;
}

export async function createAiQuickQuestion(request: AiQuickQuestionUpsertRequest): Promise<number> {
  const res = await http.post<ApiResponse<number>>("/api/v1/admin/ai-quick-questions", request);
  return res.data.data;
}

export async function updateAiQuickQuestion(id: number, request: AiQuickQuestionUpsertRequest): Promise<void> {
  await http.put<ApiResponse<unknown>>(`/api/v1/admin/ai-quick-questions/${id}`, request);
}

export async function deleteAiQuickQuestion(id: number): Promise<void> {
  await http.delete<ApiResponse<unknown>>(`/api/v1/admin/ai-quick-questions/${id}`);
}

export async function batchDeleteAiQuickQuestions(ids: number[]): Promise<void> {
  await http.post<ApiResponse<unknown>>("/api/v1/admin/ai-quick-questions/batch-delete", { ids });
}
