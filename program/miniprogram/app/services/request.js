const { API_BASE_URL, API_PREFIX, STORAGE_KEYS } = require("./config");
const { getStorage, removeStorage } = require("./storage");

class ApiError extends Error {
  constructor(message, code, traceId) {
    super(message);
    this.code = code;
    this.traceId = traceId;
  }
}

const ERROR_TOAST_DURATION_MS = 3000;

function joinUrl(base, path) {
  const trimmedBase = String(base).replace(/\/+$/, "");
  const trimmedPath = String(path).startsWith("/") ? path : `/${path}`;
  return `${trimmedBase}${trimmedPath}`;
}

function buildAuthorizationHeader() {
  const token = getStorage(STORAGE_KEYS.token);
  return token ? { Authorization: `Bearer ${token}` } : {};
}

function buildJsonHeaders() {
  return {
    "Content-Type": "application/json",
    ...buildAuthorizationHeader(),
  };
}

function handleAuthError() {
  removeStorage(STORAGE_KEYS.token);
  removeStorage(STORAGE_KEYS.me);
}

function showErrorToast(title) {
  wx.showToast({ title: title || "请求失败", icon: "none", duration: ERROR_TOAST_DURATION_MS, mask: true });
}

function buildApiUrl(path) {
  return joinUrl(API_BASE_URL, `${API_PREFIX}${path}`);
}

function apiRequest(method, path, data, options) {
  const url = buildApiUrl(path);
  const header = buildJsonHeaders();
  const toastEnabled = options?.toast !== false;

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
          if (toastEnabled) showErrorToast("服务异常");
          reject(new ApiError("Empty response", "NETWORK_ERROR"));
          return;
        }
        if (payload.code !== "OK") {
          if (payload.code === "UNAUTHORIZED") handleAuthError();
          const message = payload.message || "请求失败";
          if (toastEnabled) showErrorToast(message);
          reject(new ApiError(message, payload.code, payload.traceId));
          return;
        }
        resolve(payload.data);
      },
      fail: () => {
        if (toastEnabled) showErrorToast("网络错误");
        reject(new ApiError("Network error", "NETWORK_ERROR"));
      },
    });
  });
}

module.exports = { apiRequest, ApiError, buildApiUrl, buildAuthorizationHeader, handleAuthError, showErrorToast };
