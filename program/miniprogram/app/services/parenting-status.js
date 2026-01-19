const { apiRequest } = require("./request");

function getDailyParentingStatus(childId, recordDate) {
  const params = { childId };
  if (recordDate) params.recordDate = recordDate;
  return apiRequest("GET", "/miniprogram/parenting-status/daily", params);
}

function upsertDailyParentingStatus(payload) {
  return apiRequest("PUT", "/miniprogram/parenting-status/daily", payload);
}

module.exports = { getDailyParentingStatus, upsertDailyParentingStatus };

