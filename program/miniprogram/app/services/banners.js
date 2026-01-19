const { apiRequest } = require("./request");

function listBanners() {
  return apiRequest("GET", "/miniprogram/banners");
}

function getBannerDetail(id) {
  return apiRequest("GET", `/miniprogram/banners/${encodeURIComponent(String(id))}`);
}

module.exports = { listBanners, getBannerDetail };

