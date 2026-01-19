const { apiRequest } = require("./request");

function getDailyDiary(childId, recordDate) {
  const params = { childId };
  if (recordDate) params.recordDate = recordDate;
  return apiRequest("GET", "/miniprogram/diary/daily", params);
}

function upsertDailyDiary(payload) {
  return apiRequest("PUT", "/miniprogram/diary/daily", payload);
}

module.exports = { getDailyDiary, upsertDailyDiary };

