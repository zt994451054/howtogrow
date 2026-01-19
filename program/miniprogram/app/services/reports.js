const { apiRequest } = require("./request");

function fetchGrowthReport(childId, from, to) {
  const qs = `childId=${encodeURIComponent(String(childId))}&from=${encodeURIComponent(from)}&to=${encodeURIComponent(to)}`;
  return apiRequest("GET", `/miniprogram/reports/growth?${qs}`);
}

function fetchAwarenessPersistence(childId, options) {
  const cid = Number(childId || 0);
  if (!cid) return Promise.resolve(null);
  return apiRequest("GET", "/miniprogram/reports/persistence", { childId: cid }, options);
}

module.exports = { fetchGrowthReport, fetchAwarenessPersistence };
