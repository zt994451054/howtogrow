const API_BASE_URL = "http://api.huangruoyi.cn:8080";
const API_PREFIX = "/api/v1";
// H5 域名端口（用于小程序 WebView 打开 Banner 详情页：{H5_BASE_URL}/h5/banners/{bannerId}）
// 本地开发默认使用 Vite 端口：5173
const H5_BASE_URL = "http://web.huangruoyi.cn/";

const STORAGE_KEYS = {
  token: "auth:token",
  me: "user:me",
  children: "children:list",
  navMe: "nav:me",
  navHomeSelectedChildId: "nav:home:selectedChildId",
  navCurveSelectedChildId: "nav:curve:selectedChildId",
  dailySession: "daily:session",
  chatActiveSessionId: "chat:activeSessionId",
};

module.exports = { API_BASE_URL, API_PREFIX, H5_BASE_URL, STORAGE_KEYS };
