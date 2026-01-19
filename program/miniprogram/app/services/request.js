const { API_BASE_URL, API_PREFIX, STORAGE_KEYS } = require("./config");
const { getStorage, removeStorage, setStorage } = require("./storage");

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

function wxLogin() {
  return new Promise((resolve, reject) => {
    wx.login({
      success: (res) => (res.code ? resolve(res.code) : reject(new Error("empty code"))),
      fail: () => reject(new Error("wx.login failed")),
    });
  });
}

let reloginPromise = null;

function startRelogin() {
  if (reloginPromise) return reloginPromise;
  reloginPromise = (async () => {
    const code = await wxLogin();
    const url = buildApiUrl("/miniprogram/auth/wechat-login");
    const header = { "Content-Type": "application/json" };

    const payload = await new Promise((resolve, reject) => {
      wx.request({
        url,
        method: "POST",
        data: { code },
        header,
        timeout: 15000,
        success: (res) => resolve(res?.data),
        fail: () => reject(new ApiError("Network error", "NETWORK_ERROR")),
      });
    });

    if (!payload || payload.code !== "OK" || !payload.data) {
      const message = payload?.message || "登录失败";
      const codeValue = payload?.code || "UNAUTHORIZED";
      throw new ApiError(message, codeValue, payload?.traceId);
    }

    setStorage(STORAGE_KEYS.token, payload.data.token);
    setStorage(STORAGE_KEYS.me, payload.data.user);
    return payload.data;
  })();

  reloginPromise.finally(() => {
    // prevent holding old promise forever
    reloginPromise = null;
  });

  return reloginPromise;
}

function apiRequest(method, path, data, options) {
  const url = buildApiUrl(path);
  const header = buildJsonHeaders();
  const toastEnabled = options?.toast !== false;
  const autoRelogin = options?.autoRelogin !== false;
  const canRetry = options?.__retried !== true;

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
          if (payload.code === "UNAUTHORIZED") {
            handleAuthError();
            const isLoginApi = String(path || "") === "/miniprogram/auth/wechat-login";
            if (autoRelogin && !isLoginApi && canRetry) {
              startRelogin()
                .then(() => apiRequest(method, path, data, { ...(options || {}), __retried: true }))
                .then(resolve)
                .catch((err) => {
                  const message = err?.message || payload.message || "登录已失效，请重新登录";
                  if (toastEnabled) showErrorToast(message);
                  reject(err instanceof ApiError ? err : new ApiError(message, "UNAUTHORIZED", payload.traceId));
                });
              return;
            }
          }
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
