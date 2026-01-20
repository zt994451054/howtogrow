const { apiRequest, buildApiUrl, buildAuthorizationHeader, handleAuthError, showErrorToast } = require("./request");

function createChatSession(childId) {
  return apiRequest("POST", "/miniprogram/ai/chat/sessions", { childId: childId ?? null }).then((res) => res.sessionId);
}

function listChatSessions(limit = 20) {
  return apiRequest("GET", `/miniprogram/ai/chat/sessions?limit=${encodeURIComponent(String(limit))}`);
}

function listChatQuickQuestions(limit = 6) {
  return apiRequest("GET", `/miniprogram/ai/chat/quick-questions?limit=${encodeURIComponent(String(limit))}`);
}

function listChatMessages(sessionId, limit = 20, beforeMessageId) {
  const query = [];
  query.push(`limit=${encodeURIComponent(String(limit))}`);
  if (beforeMessageId) query.push(`beforeMessageId=${encodeURIComponent(String(beforeMessageId))}`);
  return apiRequest("GET", `/miniprogram/ai/chat/sessions/${encodeURIComponent(String(sessionId))}/messages?${query.join("&")}`);
}

function sendChatMessage(sessionId, content, options) {
  return apiRequest("POST", `/miniprogram/ai/chat/sessions/${encodeURIComponent(String(sessionId))}/messages`, { content }, options).then((r) => r.messageId);
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

function parsePossibleJson(input) {
  try {
    return JSON.parse(String(input));
  } catch {
    return null;
  }
}

function normalizeStreamErrorData(data) {
  const parsed = parsePossibleJson(data);
  if (parsed && typeof parsed === "object") {
    const message = typeof parsed.message === "string" && parsed.message.trim() ? parsed.message : "请求失败";
    const code = typeof parsed.code === "string" ? parsed.code : undefined;
    return { message, code };
  }
  return { message: String(data || "请求失败"), code: undefined };
}

function streamChat(sessionId, handlers) {
  const url = buildApiUrl(`/miniprogram/ai/chat/sessions/${encodeURIComponent(String(sessionId))}/stream`);

  let sseBuffer = "";
  let didFail = false;
  let requestTask;

  const failOnce = (message, code, requestTask) => {
    if (didFail) return;
    didFail = true;
    if (code === "UNAUTHORIZED") handleAuthError();
    showErrorToast(message || "请求失败");
    try {
      requestTask.abort();
    } catch {
      // ignore
    }
    if (typeof handlers.onError === "function") handlers.onError(message || "error");
  };

  const sseHandlers = {
    onDelta: handlers.onDelta,
    onDone: handlers.onDone,
    onError: (data) => {
      const normalized = normalizeStreamErrorData(data);
      failOnce(normalized.message, normalized.code, requestTask);
    },
  };

  requestTask = wx.request({
    url,
    method: "GET",
    header: {
      ...buildAuthorizationHeader(),
      Accept: "text/event-stream",
    },
    enableChunked: true,
    responseType: "arraybuffer",
    success: (res) => {
      if (res && typeof res.statusCode === "number" && res.statusCode >= 400) {
        let message = "请求失败";
        const normalized = normalizeStreamErrorData(
          res.data instanceof ArrayBuffer ? decodeChunk(res.data) : typeof res.data === "string" ? res.data : ""
        );
        if (normalized.message) message = normalized.message;
        const code = res.statusCode === 401 ? "UNAUTHORIZED" : normalized.code;
        failOnce(message, code, requestTask);
        return;
      }
      if (res && res.data instanceof ArrayBuffer) {
        sseBuffer += decodeChunk(res.data);
      } else if (res && typeof res.data === "string") {
        sseBuffer += res.data;
      }
      const parsed = parseSse(sseBuffer, sseHandlers);
      sseBuffer = parsed.rest;
    },
    fail: () => failOnce("网络错误", "NETWORK_ERROR", requestTask),
  });

  const supportsChunk = requestTask && typeof requestTask.onChunkReceived === "function";
  if (supportsChunk) {
    requestTask.onChunkReceived((chunkRes) => {
      if (!chunkRes || !chunkRes.data) return;
      if (chunkRes.data instanceof ArrayBuffer) {
        sseBuffer += decodeChunk(chunkRes.data);
      }
      const parsed = parseSse(sseBuffer, sseHandlers);
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

module.exports = { createChatSession, listChatSessions, listChatQuickQuestions, listChatMessages, sendChatMessage, streamChat };
