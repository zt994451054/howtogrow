const { apiRequest } = require("./request");

async function fetchRandomQuote(childId, scene, options) {
  if (!childId || !scene) return "";
  const response = await apiRequest("GET", "/miniprogram/quotes/random", { childId, scene }, options);
  const list = Array.isArray(response) ? response : [];
  return list.length ? String(list[0].content || "") : "";
}

module.exports = { fetchRandomQuote };
