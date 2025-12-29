import assert from "assert";

function normalizeBaseUrl(baseUrl) {
  const trimmed = String(baseUrl ?? "").trim();
  if (!trimmed) throw new Error("baseUrl is required");
  return trimmed.endsWith("/") ? trimmed.slice(0, -1) : trimmed;
}

function parseRuntimeConfig(cfg) {
  if (!cfg || typeof cfg !== "object") {
    throw new Error("Missing runtime config: window.__APP_CONFIG__.apiBaseUrl");
  }
  if (!("apiBaseUrl" in cfg)) {
    throw new Error("Missing runtime config: window.__APP_CONFIG__.apiBaseUrl");
  }
  return {
    env: cfg.env,
    apiBaseUrlMap: cfg.apiBaseUrlMap,
    apiBaseUrl: normalizeBaseUrl(cfg.apiBaseUrl)
  };
}

{
  assert.throws(() => parseRuntimeConfig(undefined), /Missing runtime config/);
  assert.throws(() => parseRuntimeConfig({}), /Missing runtime config/);
  const cfg = parseRuntimeConfig({ env: "dev", apiBaseUrl: "http://127.0.0.1:8080/" });
  assert.equal(cfg.apiBaseUrl, "http://127.0.0.1:8080");
}

console.log("ok: runtime config");

