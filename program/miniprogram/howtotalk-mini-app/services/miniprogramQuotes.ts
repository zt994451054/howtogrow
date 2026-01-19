export type ApiResponse<T> = {
  code: string;
  message?: string;
  data: T;
  traceId?: string;
};

export class ApiError extends Error {
  public readonly code: string;
  public readonly traceId?: string;
  public readonly httpStatus?: number;

  constructor(message: string, code: string, traceId?: string, httpStatus?: number) {
    super(message);
    this.code = code;
    this.traceId = traceId;
    this.httpStatus = httpStatus;
  }
}

function getApiBaseUrl(): string {
  const baseUrl = (import.meta as any).env?.VITE_API_BASE_URL;
  return typeof baseUrl === "string" ? baseUrl.trim() : "";
}

function getAuthToken(): string | null {
  if (typeof window === "undefined") return null;
  try {
    return window.localStorage.getItem("auth:token");
  } catch {
    return null;
  }
}

function buildUrl(pathname: string, query: Record<string, string>): string {
  const baseUrl = getApiBaseUrl();
  const base = baseUrl || (typeof window !== "undefined" ? window.location.origin : "http://localhost");
  const url = new URL(pathname, base);
  url.search = new URLSearchParams(query).toString();
  return baseUrl ? url.toString() : `${url.pathname}${url.search}${url.hash}`;
}

async function fetchApi<T>(pathname: string, query: Record<string, string>, signal?: AbortSignal): Promise<T> {
  const url = buildUrl(pathname, query);
  const token = getAuthToken();
  const res = await fetch(url, {
    method: "GET",
    headers: {
      "Content-Type": "application/json",
      ...(token ? { Authorization: `Bearer ${token}` } : {}),
    },
    signal,
  });

  const httpStatus = res.status;
  const payload = (await res.json().catch(() => null)) as ApiResponse<T> | null;
  if (!payload) {
    throw new ApiError("Empty response", "NETWORK_ERROR", undefined, httpStatus);
  }
  if (payload.code !== "OK") {
    throw new ApiError(payload.message || "请求失败", payload.code, payload.traceId, httpStatus);
  }
  return payload.data;
}

export async function fetchRandomQuote(
  childId: number,
  scene: string,
  options?: { signal?: AbortSignal },
): Promise<string | null> {
  if (!Number.isFinite(childId) || childId <= 0) return null;
  const safeScene = String(scene || "").trim();
  if (!safeScene) return null;

  const list = await fetchApi<Array<{ content?: string | null }>>(
    "/api/v1/miniprogram/quotes/random",
    { childId: String(childId), scene: safeScene },
    options?.signal,
  );
  const first = Array.isArray(list) && list.length ? list[0] : null;
  const content = first?.content ? String(first.content).trim() : "";
  return content || null;
}

