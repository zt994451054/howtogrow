import axios, { AxiosError, type AxiosInstance, type AxiosResponse } from "axios";
import { ElMessage } from "element-plus";
import { getApiBaseUrl } from "@/config/runtimeConfig";
import { useAuthStore } from "@/stores/auth";
import { usePermissionStore } from "@/stores/permission";

function isUnauthorized(error: unknown): boolean {
  return (
    error instanceof AxiosError &&
    typeof error.response?.status === "number" &&
    error.response.status === 401
  );
}

function isApiResponse(payload: unknown): payload is { code: unknown; message: unknown; traceId?: unknown } {
  return typeof payload === "object" && payload !== null && "code" in payload && "message" in payload;
}

function handleUnauthorized(): void {
  const auth = useAuthStore();
  auth.clear();

  // Clear in-memory permission cache so UI doesn't keep stale menus/buttons.
  try {
    usePermissionStore().$reset();
  } catch {
    // ignore
  }

  if (window.location.pathname !== "/login") {
    window.location.replace("/login");
  }
}

function extractTraceId(error: unknown): string | undefined {
  if (!(error instanceof AxiosError)) return undefined;
  const traceId = error.response?.headers?.["x-trace-id"];
  if (typeof traceId === "string" && traceId.length > 0) return traceId;
  return undefined;
}

function extractBusinessTraceId(response: AxiosResponse): string | undefined {
  if (!isApiResponse(response.data)) return undefined;
  const traceId = response.data.traceId;
  return typeof traceId === "string" && traceId.length > 0 ? traceId : undefined;
}

export function createHttpClient(): AxiosInstance {
  const instance = axios.create({
    baseURL: getApiBaseUrl(),
    timeout: 15_000
  });

  instance.interceptors.request.use((config) => {
    const auth = useAuthStore();
    if (auth.token) {
      config.headers = config.headers ?? {};
      config.headers.Authorization = `Bearer ${auth.token}`;
    }
    return config;
  });

  instance.interceptors.response.use(
    (res) => {
      if (isApiResponse(res.data) && res.data.code !== "OK") {
        if (res.data.code === "UNAUTHORIZED") {
          handleUnauthorized();
          return Promise.reject(new Error("UNAUTHORIZED"));
        }

        const traceId = extractBusinessTraceId(res);
        const suffix = traceId ? `（traceId: ${traceId}）` : "";
        const message = typeof res.data.message === "string" ? res.data.message : "请求失败";
        ElMessage.error(`${message}${suffix}`);
        return Promise.reject(new Error(message));
      }
      return res;
    },
    async (error) => {
      if (isUnauthorized(error)) {
        handleUnauthorized();
        return Promise.reject(error);
      }

      const traceId = extractTraceId(error);
      const suffix = traceId ? `（traceId: ${traceId}）` : "";

      // When downloading files (responseType=blob), error bodies are often returned as JSON but wrapped in Blob.
      // Parse them so users can see the real backend message.
      if (error instanceof AxiosError && error.response?.data instanceof Blob) {
        const ct = error.response.headers?.["content-type"];
        if (typeof ct === "string" && ct.toLowerCase().includes("application/json")) {
          try {
            const text = await error.response.data.text();
            const payload = JSON.parse(text);
            if (isApiResponse(payload)) {
              const message =
                typeof payload.message === "string" && payload.message.trim() ? payload.message.trim() : "请求失败";
              ElMessage.error(`${message}${suffix}`);
              return Promise.reject(error);
            }
          } catch {
            // ignore and fall back to generic handling
          }
        }
      }

      if (error instanceof AxiosError && isApiResponse(error.response?.data)) {
        const message =
          typeof error.response?.data?.message === "string" && error.response?.data?.message.trim()
            ? error.response.data.message.trim()
            : "请求失败";
        ElMessage.error(`${message}${suffix}`);
        return Promise.reject(error);
      }

      ElMessage.error(`请求失败${suffix}`);
      return Promise.reject(error);
    }
  );

  return instance;
}

export const http = createHttpClient();
