const { H5_BASE_URL } = require("../../services/config");
const { getSystemMetrics } = require("../../utils/system");

function normalizeBaseUrl(baseUrl) {
  return String(baseUrl || "")
    .trim()
    .replace(/\/+$/, "");
}

function buildBannerH5Url(baseUrl, bannerId) {
  const base = normalizeBaseUrl(baseUrl);
  if (!base) return "";
  const id = encodeURIComponent(String(bannerId));
  return `${base}/h5/banners/${id}`;
}

Page({
  data: { statusBarHeight: 20, id: 0, title: "详情", src: "" },
  onLoad(query) {
    const { statusBarHeight } = getSystemMetrics();
    const id = Number(query.id || 0);
    if (!id) {
      wx.showToast({ title: "参数错误", icon: "none" });
      return;
    }
    this.setData({ statusBarHeight, id, src: buildBannerH5Url(H5_BASE_URL, id) });
  },
  onBack() {
    wx.navigateBack();
  },
});
