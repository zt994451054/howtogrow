const { apiRequest } = require("./request");

function beginDailyAssessment(childId) {
  return apiRequest("POST", "/miniprogram/assessments/daily/begin", { childId });
}

function replaceDailyQuestion(sessionId, payload, options) {
  return apiRequest("POST", `/miniprogram/assessments/daily/sessions/${encodeURIComponent(sessionId)}/replace`, payload, options);
}

function submitDailyAssessment(sessionId, payload) {
  return apiRequest("POST", `/miniprogram/assessments/daily/sessions/${encodeURIComponent(sessionId)}/submit`, payload);
}

async function fetchAiSummary(assessmentId) {
  const res = await apiRequest("POST", `/miniprogram/assessments/daily/${assessmentId}/ai-summary`, undefined, { toast: false });
  return res.content;
}

function listDailyRecords(limit, offset) {
  return apiRequest("GET", "/miniprogram/assessments/daily/records", { limit: limit || 20, offset: offset || 0 });
}

function getDailyRecordDetail(assessmentId) {
  return apiRequest("GET", `/miniprogram/assessments/daily/records/${assessmentId}`);
}

module.exports = { beginDailyAssessment, replaceDailyQuestion, submitDailyAssessment, fetchAiSummary, listDailyRecords, getDailyRecordDetail };
