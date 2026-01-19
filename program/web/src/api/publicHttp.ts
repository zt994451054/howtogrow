import axios, { AxiosError, type AxiosInstance, type AxiosResponse } from "axios";
import { getApiBaseUrl } from "@/config/runtimeConfig";

function isApiResponse(payload: unknown): payload is { code: unknown; message: unknown } {
  return typeof payload === "object" && payload !== null && "code" in payload && "message" in payload;
}

function extractMessage(payload: unknown): string | undefined {
  if (!isApiResponse(payload)) return undefined;
  return typeof payload.message === "string" && payload.message.trim() ? payload.message.trim() : undefined;
}

function extractTraceId(error: unknown): string | undefined {
  if (!(error instanceof AxiosError)) return undefined;
  const traceId = error.response?.headers?.["x-trace-id"];
  if (typeof traceId === "string" && traceId.length > 0) return traceId;
  return undefined;
}

function extractBusinessTraceId(response: AxiosResponse): string | undefined {
  if (!isApiResponse(response.data)) return undefined;
  const traceId = (response.data as { traceId?: unknown }).traceId;
  return typeof traceId === "string" && traceId.length > 0 ? traceId : undefined;
}

export function createPublicHttpClient(): AxiosInstance {
  const instance = axios.create({
    baseURL: getApiBaseUrl(),
    timeout: 15_000
  });

  instance.interceptors.response.use(
    (res) => {
      if (isApiResponse(res.data) && res.data.code !== "OK") {
        const message = extractMessage(res.data) ?? "请求失败";
        const traceId = extractBusinessTraceId(res);
        const suffix = traceId ? ` (traceId: ${traceId})` : "";
        return Promise.reject(new Error(`${message}${suffix}`));
      }
      return res;
    },
    (error) => {
      const traceId = extractTraceId(error);
      const suffix = traceId ? ` (traceId: ${traceId})` : "";

      if (error instanceof AxiosError) {
        const message = extractMessage(error.response?.data) ?? "请求失败";
        return Promise.reject(new Error(`${message}${suffix}`));
      }

      return Promise.reject(new Error(`请求失败${suffix}`));
    }
  );

  return instance;
}

export const publicHttp = createPublicHttpClient();

