const { apiRequest } = require("./request");

async function fetchRandomQuote() {
  const response = await apiRequest("GET", "/miniprogram/quotes/random");
  return response.content;
}

module.exports = { fetchRandomQuote };

