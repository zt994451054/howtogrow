const { STORAGE_KEYS } = require("./config");
const { getStorage, setStorage } = require("./storage");
const { apiRequest } = require("./request");

function wxLogin() {
  return new Promise((resolve, reject) => {
    wx.login({
      success: (res) => (res.code ? resolve(res.code) : reject(new Error("empty code"))),
      fail: () => reject(new Error("wx.login failed")),
    });
  });
}

function getToken() {
  return getStorage(STORAGE_KEYS.token);
}

function getCachedMe() {
  return getStorage(STORAGE_KEYS.me);
}

async function login() {
  const code = await wxLogin();
  const response = await apiRequest("POST", "/miniprogram/auth/wechat-login", { code });
  setStorage(STORAGE_KEYS.token, response.token);
  setStorage(STORAGE_KEYS.me, response.user);
  return response;
}

async function fetchMe() {
  const response = await apiRequest("GET", "/miniprogram/me");
  setStorage(STORAGE_KEYS.me, response.user);
  return response.user;
}

async function ensureLoggedIn() {
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

module.exports = { ensureLoggedIn, login, fetchMe, getCachedMe, getToken };

