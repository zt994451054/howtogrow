export type ApiCode =
  | "OK"
  | "INVALID_REQUEST"
  | "UNAUTHORIZED"
  | "FORBIDDEN_RESOURCE"
  | "SUBSCRIPTION_REQUIRED"
  | "FREE_TRIAL_ALREADY_USED"
  | "DAILY_ASSESSMENT_ALREADY_SUBMITTED"
  | "DAILY_ASSESSMENT_INCOMPLETE"
  | "QUESTION_POOL_EXHAUSTED"
  | "AI_SUMMARY_ALREADY_GENERATED"
  | "RATE_LIMITED";

export interface ApiResponse<T> {
  code: ApiCode;
  message: string;
  data: T;
  traceId?: string;
}

export interface WechatLoginRequest {
  code: string;
}

export interface MiniprogramUserView {
  id: number;
  nickname: string | null;
  avatarUrl: string | null;
  subscriptionEndAt: string | null;
  freeTrialUsed: boolean;
}

export interface WechatLoginResponse {
  token: string;
  expiresIn: number;
  user: MiniprogramUserView;
}

export interface MiniprogramMeResponse {
  user: MiniprogramUserView;
}

export interface ChildView {
  id: number;
  nickname: string;
  gender: 0 | 1 | 2;
  birthDate: string;
}

export interface ChildCreateRequest {
  nickname: string;
  gender: 0 | 1 | 2;
  birthDate: string;
}

export interface ChildCreateResponse {
  childId: number;
}

export interface DailyAssessmentBeginRequest {
  childId: number;
}

export type QuestionType = "SINGLE" | "MULTI";

export interface QuestionOptionView {
  optionId: number;
  content: string;
  sortNo: number;
}

export interface DailyAssessmentItemView {
  displayOrder: number;
  questionId: number;
  content: string;
  questionType: QuestionType;
  options: QuestionOptionView[];
}

export interface DailyAssessmentBeginResponse {
  sessionId: string;
  items: DailyAssessmentItemView[];
}

export interface DailyAssessmentReplaceRequest {
  childId: number;
  displayOrder: number;
}

export interface DailyAssessmentReplaceResponse {
  displayOrder: number;
  newItem: DailyAssessmentItemView;
}

export interface DailyAssessmentAnswerRequest {
  questionId: number;
  optionIds: number[];
}

export interface DailyAssessmentSubmitRequest {
  childId: number;
  answers: DailyAssessmentAnswerRequest[];
}

export interface DimensionScoreView {
  dimensionCode: string;
  dimensionName: string;
  score: number;
}

export interface DailyAssessmentSubmitResponse {
  assessmentId: number;
  dimensionScores: DimensionScoreView[];
}

export interface AiSummaryResponse {
  content: string;
}

export interface AiChatCreateSessionRequest {
  childId: number | null;
}

export interface AiChatCreateSessionResponse {
  sessionId: number;
}

export interface AiChatSessionView {
  sessionId: number;
  childId: number | null;
  status: "ACTIVE" | "CLOSED";
  lastActiveAt: string;
}

export interface AiChatMessageCreateRequest {
  content: string;
}

export interface AiChatMessageCreateResponse {
  messageId: number;
}

export interface GrowthDayView {
  bizDate: string;
  dimensionScores: DimensionScoreView[];
}

export interface GrowthReportResponse {
  childId: number;
  from: string;
  to: string;
  days: GrowthDayView[];
}

export interface QuoteResponse {
  content: string;
}

export interface SubscriptionPlanView {
  planId: number;
  name: string;
  days: number;
  priceCent: number;
}

export interface SubscriptionOrderCreateRequest {
  planId: number;
}

export interface PayParams {
  timeStamp: string;
  nonceStr: string;
  package: string;
  signType: string;
  paySign: string;
}

export interface SubscriptionOrderCreateResponse {
  orderNo: string;
  payParams: PayParams;
}

