
export interface FeedbackData {
  scenario: string;
  yourOldWay: string;
  insightTitle: string;
  insightBody: string;
  yourNewWay: string;
}

export interface RawTestResult {
  id: string;
  date: string;
  scores: {
    dimension1: number; // e.g., Logic
    dimension2: number; // e.g., Knowledge
    dimension3: number; // e.g., Speed
    dimension4: number; // e.g., Accuracy
    dimension5: number; // e.g., Creativity
  };
  feedback?: FeedbackData;
}

export interface BatchedResult {
  batchId: number;
  completionDate: string;
  avgScores: {
    dimension1: number;
    dimension2: number;
    dimension3: number;
    dimension4: number;
    dimension5: number;
  };
  tests: RawTestResult[]; // Keep reference to individual tests in the batch
}

export const DIMENSION_NAMES = {
  dimension1: "逻辑思维 (Logic)",
  dimension2: "知识储备 (Knowledge)",
  dimension3: "反应速度 (Speed)",
  dimension4: "准确度 (Accuracy)",
  dimension5: "创造力 (Creativity)"
};

export const DIMENSION_COLORS = {
  dimension1: "#f97316", // orange-500
  dimension2: "#ec4899", // pink-500
  dimension3: "#10b981", // emerald-500
  dimension4: "#3b82f6", // blue-500
  dimension5: "#8b5cf6"  // violet-500
};
