const { apiRequest } = require("./request");

function fetchPlans() {
  return apiRequest("GET", "/miniprogram/subscriptions/plans");
}

function createOrder(payload) {
  return apiRequest("POST", "/miniprogram/subscriptions/orders", payload);
}

module.exports = { fetchPlans, createOrder };

