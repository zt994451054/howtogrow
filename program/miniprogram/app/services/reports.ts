import { apiRequest } from "./request";
import type { GrowthReportResponse } from "./types";

export async function fetchGrowthReport(childId: number, from: string, to: string): Promise<GrowthReportResponse> {
  const qs = `childId=${encodeURIComponent(String(childId))}&from=${encodeURIComponent(from)}&to=${encodeURIComponent(to)}`;
  return apiRequest<GrowthReportResponse>("GET", `/miniprogram/reports/growth?${qs}`);
}

