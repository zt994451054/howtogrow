import { http } from "@/api/http";
import type { ApiResponse, PageResponse } from "@/api/types";

export type AssessmentView = {
  assessmentId: number;
  userId: number;
  userNickname: string | null;
  userAvatarUrl: string | null;
  childId: number;
  childNickname: string | null;
  bizDate: string;
  submittedAt: string | null;
  emotionManagementScore: number;
  communicationExpressionScore: number;
  ruleGuidanceScore: number;
  relationshipBuildingScore: number;
  learningSupportScore: number;
  dimensionScores: { dimensionCode: string; dimensionName: string; score: number }[];
};

export type AssessmentListParams = {
  page: number;
  pageSize: number;
  bizDateFrom?: string;
  bizDateTo?: string;
  userId?: number;
  childId?: number;
  keyword?: string;
};

export type AssessmentExportParams = Omit<AssessmentListParams, "page" | "pageSize">;

function parseContentDispositionFilename(contentDisposition: unknown): string | undefined {
  if (typeof contentDisposition !== "string" || !contentDisposition.trim()) return undefined;

  // RFC 5987: filename*=UTF-8''...
  const star = contentDisposition.match(/filename\*\s*=\s*([^']*)''([^;]+)/i);
  if (star?.[2]) {
    try {
      return decodeURIComponent(star[2].trim());
    } catch {
      // ignore
    }
  }

  // Fallback: filename="..."
  const plain = contentDisposition.match(/filename\s*=\s*\"([^\"]+)\"/i);
  if (plain?.[1]) return plain[1].trim();

  return undefined;
}

export async function listAssessments(params: AssessmentListParams): Promise<PageResponse<AssessmentView>> {
  const res = await http.get<ApiResponse<PageResponse<AssessmentView>>>("/api/v1/admin/assessments", {
    params
  });
  return res.data.data;
}

export async function exportAssessmentsExcel(
  params: AssessmentExportParams
): Promise<{ blob: Blob; filename?: string }> {
  const res = await http.get<Blob>("/api/v1/admin/assessments/export-excel", {
    params,
    responseType: "blob"
  });
  const filename = parseContentDispositionFilename(res.headers?.["content-disposition"]);
  return { blob: res.data, filename };
}
