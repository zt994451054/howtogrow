export const API_BASE_URL = "http://127.0.0.1:8080";
export const API_PREFIX = "/api/v1";

export const STORAGE_KEYS = {
  token: "auth:token",
  me: "user:me",
  children: "children:list",
  navMe: "nav:me",
  dailySession: "daily:session",
  chatActiveSessionId: "chat:activeSessionId",
} as const;

