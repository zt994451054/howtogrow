import { API_BASE_URL, API_PREFIX, STORAGE_KEYS } from "./config";
import type { ApiResponse, ApiCode } from "./types";
import { getStorage, removeStorage } from "./storage";

export class ApiError extends Error {
  public readonly code: ApiCode | "NETWORK_ERROR";
  public readonly traceId?: string;

  constructor(message: string, code: ApiCode | "NETWORK_ERROR", traceId?: string) {
    super(message);
    this.code = code;
    this.traceId = traceId;
  }
}

type HttpMethod = "GET" | "POST" | "PUT" | "DELETE";

function joinUrl(base: string, path: string): string {
  const trimmedBase = base.replace(/\/+$/, "");
  const trimmedPath = path.startsWith("/") ? path : `/${path}`;
  return `${trimmedBase}${trimmedPath}`;
}

function buildHeaders(): Record<string, string> {
  const token = getStorage<string>(STORAGE_KEYS.token);
  const headers: Record<string, string> = {
    "Content-Type": "application/json",
  };
  if (token) {
    headers.Authorization = `Bearer ${token}`;
  }
  return headers;
}

function handleAuthError(): void {
  removeStorage(STORAGE_KEYS.token);
  removeStorage(STORAGE_KEYS.me);
}

export async function apiRequest<T>(
  method: HttpMethod,
  path: string,
  data?: unknown
): Promise<T> {
  const url = joinUrl(API_BASE_URL, `${API_PREFIX}${path}`);
  const headers = buildHeaders();

  const response = await new Promise<ApiResponse<T>>((resolve, reject) => {
    wx.request<ApiResponse<T>>({
      url,
      method,
      data,
      header: headers,
      timeout: 15000,
      success: (res) => {
        if (!res.data) {
          reject(new ApiError("Empty response", "NETWORK_ERROR"));
          return;
        }
        resolve(res.data);
      },
      fail: () => reject(new ApiError("Network error", "NETWORK_ERROR")),
    });
  });

  if (response.code !== "OK") {
    if (response.code === "UNAUTHORIZED") {
      handleAuthError();
    }
    throw new ApiError(response.message || "Request failed", response.code, response.traceId);
  }

  return response.data;
}

