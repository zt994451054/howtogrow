import { apiRequest } from "./request";
import type {
  AiSummaryResponse,
  DailyAssessmentBeginResponse,
  DailyAssessmentReplaceRequest,
  DailyAssessmentReplaceResponse,
  DailyAssessmentSubmitRequest,
  DailyAssessmentSubmitResponse,
} from "./types";

export async function beginDailyAssessment(childId: number): Promise<DailyAssessmentBeginResponse> {
  return apiRequest<DailyAssessmentBeginResponse>("POST", "/miniprogram/assessments/daily/begin", { childId });
}

export async function replaceDailyQuestion(
  sessionId: string,
  payload: DailyAssessmentReplaceRequest
): Promise<DailyAssessmentReplaceResponse> {
  return apiRequest<DailyAssessmentReplaceResponse>(
    "POST",
    `/miniprogram/assessments/daily/sessions/${encodeURIComponent(sessionId)}/replace`,
    payload
  );
}

export async function submitDailyAssessment(
  sessionId: string,
  payload: DailyAssessmentSubmitRequest
): Promise<DailyAssessmentSubmitResponse> {
  return apiRequest<DailyAssessmentSubmitResponse>(
    "POST",
    `/miniprogram/assessments/daily/sessions/${encodeURIComponent(sessionId)}/submit`,
    payload
  );
}

export async function fetchAiSummary(assessmentId: number): Promise<string> {
  const res = await apiRequest<AiSummaryResponse>("POST", `/miniprogram/assessments/daily/${assessmentId}/ai-summary`);
  return res.content;
}

