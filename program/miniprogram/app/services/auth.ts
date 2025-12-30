import { STORAGE_KEYS } from "./config";
import { getStorage, setStorage } from "./storage";
import { apiRequest } from "./request";
import type { MiniprogramUserView, MiniprogramMeResponse, WechatLoginResponse } from "./types";

function wxLogin(): Promise<string> {
  return new Promise((resolve, reject) => {
    wx.login({
      success: (res) => {
        if (res.code) resolve(res.code);
        else reject(new Error("empty code"));
      },
      fail: () => reject(new Error("wx.login failed")),
    });
  });
}

export function getToken(): string | null {
  return getStorage<string>(STORAGE_KEYS.token);
}

export function getCachedMe(): MiniprogramUserView | null {
  return getStorage<MiniprogramUserView>(STORAGE_KEYS.me);
}

export async function login(): Promise<WechatLoginResponse> {
  const code = await wxLogin();
  const response = await apiRequest<WechatLoginResponse>("POST", "/miniprogram/auth/wechat-login", { code });
  setStorage(STORAGE_KEYS.token, response.token);
  setStorage(STORAGE_KEYS.me, response.user);
  return response;
}

export async function fetchMe(): Promise<MiniprogramUserView> {
  const response = await apiRequest<MiniprogramMeResponse>("GET", "/miniprogram/me");
  setStorage(STORAGE_KEYS.me, response.user);
  return response.user;
}

export async function ensureLoggedIn(): Promise<MiniprogramUserView | null> {
  const existing = getToken();
  if (!existing) {
    const loginRes = await login();
    return loginRes.user;
  }
  try {
    return await fetchMe();
  } catch {
    const loginRes = await login();
    return loginRes.user;
  }
}
