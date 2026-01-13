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

export async function listAssessments(params: { page: number; pageSize: number }): Promise<PageResponse<AssessmentView>> {
  const res = await http.get<ApiResponse<PageResponse<AssessmentView>>>("/api/v1/admin/assessments", { params });
  return res.data.data;
}
