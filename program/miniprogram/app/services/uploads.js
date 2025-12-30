const { API_BASE_URL, API_PREFIX, STORAGE_KEYS } = require("./config");
const { getStorage } = require("./storage");

function uploadAvatar(filePath) {
  const normalized = filePath ? String(filePath).trim() : "";
  if (!normalized) {
    return Promise.reject(new Error("filePath required"));
  }

  const token = getStorage(STORAGE_KEYS.token);
  const url = `${API_BASE_URL}${API_PREFIX}/miniprogram/uploads/avatar`;

  return new Promise((resolve, reject) => {
    console.log('--上传---')
    wx.uploadFile({
      url,
      filePath: normalized,
      name: "file",
      header: token ? { Authorization: `Bearer ${token}` } : {},
      success: (res) => {
        if (!res || typeof res.statusCode !== "number") {
          reject(new Error("upload failed"));
          return;
        }
        if (res.statusCode < 200 || res.statusCode >= 300) {
          reject(new Error(`upload http ${res.statusCode}`));
          return;
        }
        let parsed;
        try {
          parsed = typeof res.data === "string" ? JSON.parse(res.data) : res.data;
        } catch {
          reject(new Error("invalid upload response"));
          return;
        }
        if (!parsed || parsed.code !== "OK") {
          reject(new Error((parsed && parsed.message) || "upload failed"));
          return;
        }
        const payload = parsed && parsed.data ? parsed.data : null;
        const uploadedUrl = payload && payload.url ? String(payload.url).trim() : "";
        if (!uploadedUrl) {
          reject(new Error("upload url missing"));
          return;
        }
        resolve(uploadedUrl);
      },
      fail: (err) => reject(err || new Error("upload failed")),
    });
  });
}

module.exports = { uploadAvatar };
