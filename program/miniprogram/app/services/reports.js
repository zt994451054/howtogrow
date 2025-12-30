const { apiRequest } = require("./request");

function fetchGrowthReport(childId, from, to) {
  const qs = `childId=${encodeURIComponent(String(childId))}&from=${encodeURIComponent(from)}&to=${encodeURIComponent(to)}`;
  return apiRequest("GET", `/miniprogram/reports/growth?${qs}`);
}

module.exports = { fetchGrowthReport };

