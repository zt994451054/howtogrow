const { apiRequest } = require("./request");

function listTroubleScenes() {
  return apiRequest("GET", "/miniprogram/trouble-scenes");
}

module.exports = { listTroubleScenes };

