const { ensureLoggedIn, fetchMe, getCachedMe, updateProfile } = require("../../services/auth");
const { uploadAvatar } = require("../../services/uploads");

function safeText(value) {
  return value == null ? "" : String(value);
}

function isYmd(value) {
  return /^\d{4}-\d{2}-\d{2}$/.test(safeText(value).trim());
}

function normalizeYmd(value) {
  const v = safeText(value).trim();
  return isYmd(v) ? v : "";
}

function todayYmd() {
  return new Date().toISOString().slice(0, 10);
}

Page({
  data: {
    nickname: "",
    avatarUrl: "",
    birthDate: "",
    saving: false,
    uploading: false,
    today: todayYmd(),
  },

  onLoad() {
    const cached = getCachedMe();
    if (cached) this.applyMe(cached);
    ensureLoggedIn()
      .then(() => fetchMe())
      .then((me) => this.applyMe(me))
      .catch(() => wx.showToast({ title: "加载失败", icon: "none" }));
  },

  onBack() {
    wx.navigateBack();
  },

  applyMe(me) {
    const nickname = safeText(me?.nickname).trim();
    const avatarUrl = safeText(me?.avatarUrl).trim();
    const birthDate = normalizeYmd(me?.birthDate);
    this.setData({
      nickname: nickname || "",
      avatarUrl: avatarUrl || "",
      birthDate,
    });
  },

  onNicknameInput(e) {
    const value = safeText(e?.detail?.value);
    this.setData({ nickname: value });
  },

  onPickBirthDate(e) {
    const value = safeText(e?.detail?.value).trim();
    this.setData({ birthDate: normalizeYmd(value) });
  },

  onPickAvatar() {
    if (this.data.uploading) return;
    wx.chooseImage({
      count: 1,
      sizeType: ["compressed"],
      sourceType: ["album", "camera"],
      success: (res) => {
        const path = res?.tempFilePaths && res.tempFilePaths[0] ? String(res.tempFilePaths[0]) : "";
        if (!path) return;
        this.setData({ uploading: true });
        wx.showLoading({ title: "上传中…" });
        ensureLoggedIn()
          .then(() => uploadAvatar(path))
          .then((url) => {
            this.setData({ avatarUrl: safeText(url).trim() });
          })
          .catch((err) => {
            const msg = err && err.message ? String(err.message) : "上传失败";
            wx.showToast({ title: msg, icon: "none" });
          })
          .finally(() => {
            wx.hideLoading();
            this.setData({ uploading: false });
          });
      },
    });
  },

  onChooseAvatar(e) {
    const tempPath = safeText(e?.detail?.avatarUrl).trim();
    if (!tempPath) {
      wx.showToast({ title: "获取失败，请重试", icon: "none" });
      return;
    }
    if (this.data.uploading || this.data.saving) return;

    this.setData({ uploading: true });
    wx.showLoading({ title: "上传中…" });
    ensureLoggedIn()
      .then(() => uploadAvatar(tempPath))
      .then((url) => {
        this.setData({ avatarUrl: safeText(url).trim() });
        wx.showToast({ title: "头像已更新", icon: "success" });
      })
      .catch((err) => {
        const msg = err && err.message ? String(err.message) : "上传失败";
        wx.showToast({ title: msg, icon: "none" });
      })
      .finally(() => {
        wx.hideLoading();
        this.setData({ uploading: false });
      });
  },

  onSave() {
    const nickname = safeText(this.data.nickname).trim();
    const avatarUrl = safeText(this.data.avatarUrl).trim();
    const birthDate = normalizeYmd(this.data.birthDate);
    if (!nickname) {
      wx.showToast({ title: "请输入昵称", icon: "none" });
      return;
    }
    if (!avatarUrl) {
      wx.showToast({ title: "请设置头像", icon: "none" });
      return;
    }
    if (birthDate && birthDate > this.data.today) {
      wx.showToast({ title: "出生日期不能是未来时间", icon: "none" });
      return;
    }
    if (this.data.saving) return;

    this.setData({ saving: true });
    ensureLoggedIn()
      .then(() => updateProfile({ nickname, avatarUrl, birthDate: birthDate || null }))
      .then(() => {
        wx.showToast({ title: "已保存", icon: "success" });
        setTimeout(() => wx.navigateBack(), 300);
      })
      .catch((err) => {
        const msg = err && err.message ? String(err.message) : "保存失败";
        wx.showToast({ title: msg, icon: "none" });
      })
      .finally(() => this.setData({ saving: false }));
  },
});
