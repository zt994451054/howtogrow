const { API_BASE_URL, API_PREFIX, STORAGE_KEYS } = require("./config");
const { getStorage } = require("./storage");
const { apiRequest } = require("./request");

function createChatSession(childId) {
  return apiRequest("POST", "/miniprogram/ai/chat/sessions", { childId: childId ?? null }).then((res) => res.sessionId);
}

function listChatSessions(limit = 20) {
  return apiRequest("GET", `/miniprogram/ai/chat/sessions?limit=${encodeURIComponent(String(limit))}`);
}

function sendChatMessage(sessionId, content) {
  return apiRequest("POST", `/miniprogram/ai/chat/sessions/${encodeURIComponent(String(sessionId))}/messages`, { content }).then((r) => r.messageId);
}

function joinUrl(base, path) {
  const trimmedBase = String(base).replace(/\/+$/, "");
  const trimmedPath = String(path).startsWith("/") ? path : `/${path}`;
  return `${trimmedBase}${trimmedPath}`;
}

function decodeChunk(arrayBuffer) {
  try {
    if (typeof TextDecoder !== "undefined") {
      const decoder = new TextDecoder("utf-8");
      return decoder.decode(new Uint8Array(arrayBuffer));
    }
  } catch {
    // ignore
  }
  try {
    const bytes = new Uint8Array(arrayBuffer);
    let s = "";
    for (let i = 0; i < bytes.length; i++) s += String.fromCharCode(bytes[i]);
    return decodeURIComponent(escape(s));
  } catch {
    return "";
  }
}

function parseSse(buffer, handlers) {
  const parts = String(buffer).split("\n\n");
  const complete = parts.slice(0, -1);
  const rest = parts[parts.length - 1] || "";

  complete.forEach((part) => {
    const lines = part.split("\n").filter(Boolean);
    let event = "message";
    const dataLines = [];
    lines.forEach((line) => {
      if (line.startsWith("event:")) event = line.slice("event:".length).trim();
      else if (line.startsWith("data:")) dataLines.push(line.slice("data:".length).trimStart());
    });
    const data = dataLines.join("\n");
    if (event === "delta") handlers.onDelta(data);
    else if (event === "done") handlers.onDone();
    else if (event === "error") handlers.onError(data || "error");
  });

  return { rest };
}

function streamChat(sessionId, handlers) {
  const token = getStorage(STORAGE_KEYS.token);
  const url = joinUrl(API_BASE_URL, `${API_PREFIX}/miniprogram/ai/chat/sessions/${encodeURIComponent(String(sessionId))}/stream`);

  let sseBuffer = "";
  const requestTask = wx.request({
    url,
    method: "GET",
    header: {
      ...(token ? { Authorization: `Bearer ${token}` } : {}),
      Accept: "text/event-stream",
    },
    enableChunked: true,
    responseType: "arraybuffer",
    success: (res) => {
      if (res && res.data instanceof ArrayBuffer) {
        sseBuffer += decodeChunk(res.data);
      } else if (res && typeof res.data === "string") {
        sseBuffer += res.data;
      }
      const parsed = parseSse(sseBuffer, handlers);
      sseBuffer = parsed.rest;
    },
    fail: () => handlers.onError("network error"),
  });

  const supportsChunk = requestTask && typeof requestTask.onChunkReceived === "function";
  if (supportsChunk) {
    requestTask.onChunkReceived((chunkRes) => {
      if (!chunkRes || !chunkRes.data) return;
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

module.exports = { createChatSession, listChatSessions, sendChatMessage, streamChat };

