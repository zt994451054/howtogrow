const API_BASE_URL = "http://192.168.1.152:8080";
const API_PREFIX = "/api/v1";

const STORAGE_KEYS = {
  token: "auth:token",
  me: "user:me",
  children: "children:list",
  navMe: "nav:me",
  dailySession: "daily:session",
  chatActiveSessionId: "chat:activeSessionId",
};

module.exports = { API_BASE_URL, API_PREFIX, STORAGE_KEYS };

