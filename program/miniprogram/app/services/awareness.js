const { apiRequest } = require("./request");

function getMonthlyAwareness(childId, month, options) {
  const cid = Number(childId || 0);
  const m = String(month || "").trim();
  if (!cid || !m) return Promise.resolve(null);
  return apiRequest("GET", "/miniprogram/awareness/monthly", { childId: cid, month: m }, options);
}

module.exports = { getMonthlyAwareness };
