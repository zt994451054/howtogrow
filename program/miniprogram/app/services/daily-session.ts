import { STORAGE_KEYS } from "./config";
import { getStorage, removeStorage, setStorage } from "./storage";
import type { DailyAssessmentItemView, DailyAssessmentSubmitResponse } from "./types";

export interface DailySession {
  sessionId: string;
  childId: number;
  childName: string;
  items: DailyAssessmentItemView[];
  answers: Record<number, number[]>; // questionId -> optionIds
  submitResult: DailyAssessmentSubmitResponse | null;
  assessmentId: number | null;
}

export function getDailySession(): DailySession | null {
  return getStorage<DailySession>(STORAGE_KEYS.dailySession);
}

export function setDailySession(session: DailySession): void {
  setStorage(STORAGE_KEYS.dailySession, session);
}

export function clearDailySession(): void {
  removeStorage(STORAGE_KEYS.dailySession);
}

