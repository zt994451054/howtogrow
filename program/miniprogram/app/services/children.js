const { STORAGE_KEYS } = require("./config");
const { getStorage, setStorage } = require("./storage");
const { apiRequest } = require("./request");

function getCachedChildren() {
  return getStorage(STORAGE_KEYS.children);
}

async function fetchChildren() {
  const response = await apiRequest("GET", "/miniprogram/children");
  setStorage(STORAGE_KEYS.children, response);
  return response;
}

async function createChild(payload) {
  const response = await apiRequest("POST", "/miniprogram/children", payload);
  return response.childId;
}

module.exports = { getCachedChildren, fetchChildren, createChild };

