import { http } from "@/api/http";
import type { ApiResponse, PageResponse } from "@/api/types";

export type QuestionType = "SINGLE" | "MULTI";

export type QuestionSummaryView = {
  questionId: number;
  minAge: number;
  maxAge: number;
  questionType: QuestionType;
  status: number;
  content: string;
};

export type DimensionScoreView = {
  dimensionCode: string;
  dimensionName: string;
  score: number;
};

export type OptionView = {
  optionId: number;
  content: string;
  suggestFlag: number;
  improvementTip: string | null;
  sortNo: number;
  dimensionScores: DimensionScoreView[];
};

export type QuestionDetailView = {
  questionId: number;
  minAge: number;
  maxAge: number;
  questionType: QuestionType;
  content: string;
  troubleSceneIds: number[];
  options: OptionView[];
};

export type DimensionScoreUpsert = {
  dimensionCode: string;
  score: number;
};

export type OptionUpsert = {
  content: string;
  suggestFlag: number;
  improvementTip: string | null;
  sortNo?: number;
  dimensionScores: DimensionScoreUpsert[];
};

export type QuestionUpsertRequest = {
  minAge: number;
  maxAge: number;
  questionType: QuestionType;
  content: string;
  troubleSceneIds?: number[];
  status: number;
  options: OptionUpsert[];
};

export type QuestionImportResponse = {
  total: number;
  success: number;
  failed: number;
  failures: { row: number; reason: string }[];
};

export type ListQuestionsParams = {
  page: number;
  pageSize: number;
  ageYear?: number;
  keyword?: string;
  status?: number;
  questionType?: QuestionType;
  troubleSceneId?: number;
};

export async function listQuestions(params: ListQuestionsParams): Promise<PageResponse<QuestionSummaryView>> {
  const res = await http.get<ApiResponse<PageResponse<QuestionSummaryView>>>("/api/v1/admin/questions", {
    params
  });
  return res.data.data;
}

export async function getQuestion(questionId: number): Promise<QuestionDetailView> {
  const res = await http.get<ApiResponse<QuestionDetailView>>(`/api/v1/admin/questions/${questionId}`);
  return res.data.data;
}

export async function createQuestion(request: QuestionUpsertRequest): Promise<void> {
  await http.post<ApiResponse<unknown>>("/api/v1/admin/questions", request);
}

export async function updateQuestion(questionId: number, request: QuestionUpsertRequest): Promise<void> {
  await http.put<ApiResponse<unknown>>(`/api/v1/admin/questions/${questionId}`, request);
}

export async function deleteQuestion(questionId: number): Promise<void> {
  await http.delete<ApiResponse<unknown>>(`/api/v1/admin/questions/${questionId}`);
}

export async function batchDeleteQuestions(ids: number[]): Promise<void> {
  await http.post<ApiResponse<unknown>>("/api/v1/admin/questions/batch-delete", { ids });
}

export async function importQuestionsExcel(file: File): Promise<QuestionImportResponse> {
  const form = new FormData();
  form.append("file", file);
  const res = await http.post<ApiResponse<QuestionImportResponse>>("/api/v1/admin/questions/import-excel", form, {
    headers: { "Content-Type": "multipart/form-data" }
  });
  return res.data.data;
}
