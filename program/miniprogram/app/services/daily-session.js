const { STORAGE_KEYS } = require("./config");
const { getStorage, removeStorage, setStorage } = require("./storage");

function getDailySession() {
  return getStorage(STORAGE_KEYS.dailySession);
}

function setDailySession(session) {
  setStorage(STORAGE_KEYS.dailySession, session);
}

function clearDailySession() {
  removeStorage(STORAGE_KEYS.dailySession);
}

module.exports = { getDailySession, setDailySession, clearDailySession };

