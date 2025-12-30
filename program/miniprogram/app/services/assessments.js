const { apiRequest } = require("./request");

function beginDailyAssessment(childId) {
  return apiRequest("POST", "/miniprogram/assessments/daily/begin", { childId });
}

function replaceDailyQuestion(sessionId, payload) {
  return apiRequest("POST", `/miniprogram/assessments/daily/sessions/${encodeURIComponent(sessionId)}/replace`, payload);
}

function submitDailyAssessment(sessionId, payload) {
  return apiRequest("POST", `/miniprogram/assessments/daily/sessions/${encodeURIComponent(sessionId)}/submit`, payload);
}

async function fetchAiSummary(assessmentId) {
  const res = await apiRequest("POST", `/miniprogram/assessments/daily/${assessmentId}/ai-summary`);
  return res.content;
}

module.exports = { beginDailyAssessment, replaceDailyQuestion, submitDailyAssessment, fetchAiSummary };

