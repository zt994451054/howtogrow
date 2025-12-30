import { apiRequest } from "./request";
import type { QuoteResponse } from "./types";

export async function fetchRandomQuote(): Promise<string> {
  const response = await apiRequest<QuoteResponse>("GET", "/miniprogram/quotes/random");
  return response.content;
}

