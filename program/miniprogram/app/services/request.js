const { API_BASE_URL, API_PREFIX, STORAGE_KEYS } = require("./config");
const { getStorage, removeStorage } = require("./storage");

class ApiError extends Error {
  constructor(message, code, traceId) {
    super(message);
    this.code = code;
    this.traceId = traceId;
  }
}

function joinUrl(base, path) {
  const trimmedBase = String(base).replace(/\/+$/, "");
  const trimmedPath = String(path).startsWith("/") ? path : `/${path}`;
  return `${trimmedBase}${trimmedPath}`;
}

function buildHeaders() {
  const token = getStorage(STORAGE_KEYS.token);
  const headers = { "Content-Type": "application/json" };
  if (token) headers.Authorization = `Bearer ${token}`;
  return headers;
}

function handleAuthError() {
  removeStorage(STORAGE_KEYS.token);
  removeStorage(STORAGE_KEYS.me);
}

function apiRequest(method, path, data) {
  const url = joinUrl(API_BASE_URL, `${API_PREFIX}${path}`);
  const header = buildHeaders();

  return new Promise((resolve, reject) => {
    wx.request({
      url,
      method,
      data,
      header,
      timeout: 15000,
      success: (res) => {
        const payload = res.data;
        if (!payload) {
          reject(new ApiError("Empty response", "NETWORK_ERROR"));
          return;
        }
        if (payload.code !== "OK") {
          if (payload.code === "UNAUTHORIZED") handleAuthError();
          reject(new ApiError(payload.message || "Request failed", payload.code, payload.traceId));
          return;
        }
        resolve(payload.data);
      },
      fail: () => reject(new ApiError("Network error", "NETWORK_ERROR")),
    });
  });
}

module.exports = { apiRequest, ApiError };

