const { apiRequest } = require("./request");

async function fetchChildren() {
  return apiRequest("GET", "/miniprogram/children");
}

async function createChild(payload) {
  const response = await apiRequest("POST", "/miniprogram/children", payload);
  return response.childId;
}

async function updateChild(childId, payload) {
  await apiRequest("PUT", `/miniprogram/children/${encodeURIComponent(String(childId))}`, payload);
}

module.exports = { fetchChildren, createChild, updateChild };
