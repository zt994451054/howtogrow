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

function toUint8Array(data) {
  if (!data) return null;
  if (data instanceof ArrayBuffer) return new Uint8Array(data);
  if (typeof ArrayBuffer !== "undefined" && typeof ArrayBuffer.isView === "function" && ArrayBuffer.isView(data) && data.buffer instanceof ArrayBuffer) {
    return new Uint8Array(data.buffer, data.byteOffset || 0, data.byteLength || 0);
  }
  return null;
}

function decodeUtf8BytesWithRemainder(bytes, flush) {
  const input = bytes instanceof Uint8Array ? bytes : new Uint8Array(bytes || []);
  const len = input.length;
  let i = 0;
  let out = "";

  const isCont = (b) => b >= 0x80 && b <= 0xbf;

  while (i < len) {
    const b0 = input[i];
    if (b0 <= 0x7f) {
      out += String.fromCharCode(b0);
      i += 1;
      continue;
    }

    // 2-byte
    if (b0 >= 0xc2 && b0 <= 0xdf) {
      if (i + 1 >= len) break;
      const b1 = input[i + 1];
      if (!isCont(b1)) {
        out += "\uFFFD";
        i += 1;
        continue;
      }
      const code = ((b0 & 0x1f) << 6) | (b1 & 0x3f);
      out += String.fromCharCode(code);
      i += 2;
      continue;
    }

    // 3-byte
    if (b0 >= 0xe0 && b0 <= 0xef) {
      if (i + 2 >= len) break;
      const b1 = input[i + 1];
      const b2 = input[i + 2];
      if (!isCont(b1) || !isCont(b2)) {
        out += "\uFFFD";
        i += 1;
        continue;
      }
      const code = ((b0 & 0x0f) << 12) | ((b1 & 0x3f) << 6) | (b2 & 0x3f);
      out += String.fromCharCode(code);
      i += 3;
      continue;
    }

    // 4-byte
    if (b0 >= 0xf0 && b0 <= 0xf4) {
      if (i + 3 >= len) break;
      const b1 = input[i + 1];
      const b2 = input[i + 2];
      const b3 = input[i + 3];
      if (!isCont(b1) || !isCont(b2) || !isCont(b3)) {
        out += "\uFFFD";
        i += 1;
        continue;
      }
      const codePoint = ((b0 & 0x07) << 18) | ((b1 & 0x3f) << 12) | ((b2 & 0x3f) << 6) | (b3 & 0x3f);
      const t = codePoint - 0x10000;
      out += String.fromCharCode(0xd800 + (t >> 10), 0xdc00 + (t & 0x3ff));
      i += 4;
      continue;
    }

    out += "\uFFFD";
    i += 1;
  }

  if (i >= len) return { text: out, remainingBytes: new Uint8Array(0) };
  if (flush) {
    out += "\uFFFD";
    return { text: out, remainingBytes: new Uint8Array(0) };
  }
  return { text: out, remainingBytes: input.slice(i) };
}

function decodeUtf8Bytes(bytes) {
  return decodeUtf8BytesWithRemainder(bytes, true).text;
}

function decodeUtf8Once(data) {
  if (typeof data === "string") return data;
  const bytes = toUint8Array(data);
  if (!bytes || bytes.byteLength <= 0) return "";
  try {
    if (typeof TextDecoder !== "undefined") return new TextDecoder("utf-8").decode(bytes);
  } catch {
    // ignore
  }
  return decodeUtf8Bytes(bytes);
}

function createUtf8StreamDecoder() {
  let textDecoder = null;
  try {
    if (typeof TextDecoder !== "undefined") textDecoder = new TextDecoder("utf-8");
  } catch {
    textDecoder = null;
  }

  let carry = new Uint8Array(0);

  return {
    decode: (data) => {
      const bytes = data instanceof Uint8Array ? data : toUint8Array(data);
      if (!bytes || bytes.byteLength <= 0) return "";
      if (textDecoder) {
        try {
          return textDecoder.decode(bytes, { stream: true });
        } catch {
          return textDecoder.decode(bytes);
        }
      }
      const merged = new Uint8Array(carry.length + bytes.length);
      merged.set(carry, 0);
      merged.set(bytes, carry.length);
      const decoded = decodeUtf8BytesWithRemainder(merged, false);
      carry = decoded.remainingBytes;
      return decoded.text;
    },
    flush: () => {
      if (textDecoder) {
        try {
          return textDecoder.decode();
        } catch {
          return "";
        }
      }
      if (!carry.length) return "";
      const text = decodeUtf8Bytes(carry);
      carry = new Uint8Array(0);
      return text;
    },
  };
}

function parseSse(buffer, handlers) {
  const normalized = String(buffer).replace(/\r\n/g, "\n").replace(/\r/g, "\n");
  const parts = normalized.split("\n\n");
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

function parseSseFinal(buffer, handlers) {
  // Some clients may deliver the final SSE frame without trailing "\n\n".
  // Treat the remaining buffer as complete when the HTTP request finishes.
  const first = parseSse(buffer, handlers);
  if (first.rest && first.rest.trim()) {
    const flushed = parseSse(first.rest + "\n\n", handlers);
    return { rest: flushed.rest };
  }
  return first;
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
  const decoder = createUtf8StreamDecoder();

  let sseBuffer = "";
  let didFail = false;
  let didSeeDone = false;
  let didSeeAnyDelta = false;
  let requestTask;

  const appendSseData = (data) => {
    const bytes = toUint8Array(data);
    if (bytes && bytes.byteLength > 0) {
      sseBuffer += decoder.decode(bytes);
      return;
    }
    if (typeof data === "string") {
      sseBuffer += data;
    }
  };

  const failOnce = (message, code, requestTask) => {
    if (didFail) return;
    didFail = true;
    if (code === "UNAUTHORIZED") handleAuthError();
    if (code !== "NETWORK_ERROR") showErrorToast(message || "请求失败");
    try {
      requestTask.abort();
    } catch {
      // ignore
    }
    if (typeof handlers.onError === "function") handlers.onError(message || "error", code);
  };

  const sseHandlers = {
    onDelta: (d) => {
      didSeeAnyDelta = didSeeAnyDelta || Boolean(String(d || ""));
      if (typeof handlers.onDelta === "function") handlers.onDelta(d);
    },
    onDone: () => {
      didSeeDone = true;
      if (typeof handlers.onDone === "function") handlers.onDone();
    },
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
    dataType: "other",
    enableChunked: true,
    enableHttp2: false,
    enableQuic: false,
    responseType: "arraybuffer",
    timeout: 120000,
    success: (res) => {
      if (res && typeof res.statusCode === "number" && res.statusCode >= 400) {
        let message = "请求失败";
        const normalized = normalizeStreamErrorData(
          res.data instanceof ArrayBuffer || (typeof ArrayBuffer !== "undefined" && typeof ArrayBuffer.isView === "function" && ArrayBuffer.isView(res.data))
            ? decodeUtf8Once(res.data)
            : typeof res.data === "string"
              ? res.data
              : ""
        );
        if (normalized.message) message = normalized.message;
        const code = res.statusCode === 401 ? "UNAUTHORIZED" : normalized.code;
        failOnce(message, code, requestTask);
        return;
      }
      if (res) appendSseData(res.data);
      sseBuffer += decoder.flush();
      const parsed = parseSseFinal(sseBuffer, sseHandlers);
      sseBuffer = parsed.rest;

      // If the request finishes but the final "done" frame is missing, best-effort finalize
      // so the UI won't stay stuck in typing state.
      if (!didFail && !didSeeDone && didSeeAnyDelta && typeof handlers.onDone === "function") {
        handlers.onDone();
      }
    },
    fail: () => failOnce("网络错误", "NETWORK_ERROR", requestTask),
  });

  const supportsChunk = requestTask && typeof requestTask.onChunkReceived === "function";
  if (supportsChunk) {
    requestTask.onChunkReceived((chunkRes) => {
      if (!chunkRes || !chunkRes.data) return;
      appendSseData(chunkRes.data);
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
