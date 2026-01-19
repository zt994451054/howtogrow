const { apiRequest } = require("./request");

function getDailyTroubleRecord(childId, recordDate) {
  const params = { childId };
  if (recordDate) params.recordDate = recordDate;
  return apiRequest("GET", "/miniprogram/troubles/daily", params);
}

function upsertDailyTroubleRecord(payload) {
  return apiRequest("PUT", "/miniprogram/troubles/daily", payload);
}

module.exports = { getDailyTroubleRecord, upsertDailyTroubleRecord };

