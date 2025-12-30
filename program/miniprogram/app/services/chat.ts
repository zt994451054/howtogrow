import { apiRequest } from "./request";
import type {
  AiChatCreateSessionRequest,
  AiChatCreateSessionResponse,
  AiChatMessageCreateRequest,
  AiChatMessageCreateResponse,
  AiChatSessionView,
} from "./types";
import { API_BASE_URL, API_PREFIX, STORAGE_KEYS } from "./config";
import { getStorage } from "./storage";

export async function createChatSession(childId: number | null): Promise<number> {
  const res = await apiRequest<AiChatCreateSessionResponse>("POST", "/miniprogram/ai/chat/sessions", {
    childId,
  } satisfies AiChatCreateSessionRequest);
  return res.sessionId;
}

export async function listChatSessions(limit = 20): Promise<AiChatSessionView[]> {
  return apiRequest<AiChatSessionView[]>("GET", `/miniprogram/ai/chat/sessions?limit=${encodeURIComponent(String(limit))}`);
}

export async function sendChatMessage(sessionId: number, content: string): Promise<number> {
  const res = await apiRequest<AiChatMessageCreateResponse>(
    "POST",
    `/miniprogram/ai/chat/sessions/${encodeURIComponent(String(sessionId))}/messages`,
    { content } satisfies AiChatMessageCreateRequest
  );
  return res.messageId;
}

type StreamHandlers = {
  onDelta: (text: string) => void;
  onDone: () => void;
  onError: (message: string) => void;
};

function joinUrl(base: string, path: string): string {
  const trimmedBase = base.replace(/\/+$/, "");
  const trimmedPath = path.startsWith("/") ? path : `/${path}`;
  return `${trimmedBase}${trimmedPath}`;
}

function decodeChunk(chunk: ArrayBuffer): string {
  try {
    // @ts-ignore
    const decoder = new TextDecoder("utf-8");
    // @ts-ignore
    return decoder.decode(new Uint8Array(chunk));
  } catch {
    return "";
  }
}

function parseSse(buffer: string, handlers: StreamHandlers): { rest: string } {
  const parts = buffer.split("\n\n");
  const complete = parts.slice(0, -1);
  const rest = parts[parts.length - 1] ?? "";

  for (const part of complete) {
    const lines = part.split("\n").filter(Boolean);
    let event = "message";
    const dataLines: string[] = [];
    for (const line of lines) {
      if (line.startsWith("event:")) {
        event = line.slice("event:".length).trim();
      } else if (line.startsWith("data:")) {
        dataLines.push(line.slice("data:".length).trimStart());
      }
    }
    const data = dataLines.join("\n");
    if (event === "delta") {
      handlers.onDelta(data);
    } else if (event === "done") {
      handlers.onDone();
    } else if (event === "error") {
      handlers.onError(data || "error");
    }
  }
  return { rest };
}

export function streamChat(
  sessionId: number,
  handlers: StreamHandlers
): { cancel: () => void; supportsChunk: boolean } {
  const token = getStorage<string>(STORAGE_KEYS.token);
  const url = joinUrl(API_BASE_URL, `${API_PREFIX}/miniprogram/ai/chat/sessions/${encodeURIComponent(String(sessionId))}/stream`);

  let sseBuffer = "";
  const requestTask = wx.request({
    url,
    method: "GET",
    header: {
      ...(token ? { Authorization: `Bearer ${token}` } : {}),
      Accept: "text/event-stream",
    },
    enableChunked: true as any,
    responseType: "arraybuffer" as any,
    success: (res: any) => {
      if (res?.data instanceof ArrayBuffer) {
        sseBuffer += decodeChunk(res.data);
      } else if (typeof res?.data === "string") {
        sseBuffer += res.data;
      }
      const parsed = parseSse(sseBuffer, handlers);
      sseBuffer = parsed.rest;
    },
    fail: () => handlers.onError("network error"),
  }) as any;

  const supportsChunk = typeof requestTask?.onChunkReceived === "function";
  if (supportsChunk) {
    requestTask.onChunkReceived((chunkRes: any) => {
      if (!chunkRes?.data) return;
      if (chunkRes.data instanceof ArrayBuffer) {
        sseBuffer += decodeChunk(chunkRes.data);
      }
      const parsed = parseSse(sseBuffer, handlers);
      sseBuffer = parsed.rest;
    });
  }

  return {
    supportsChunk,
    cancel: () => {
      try {
        requestTask.abort();
      } catch {
        // ignore
      }
    },
  };
}

