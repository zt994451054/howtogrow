#!/bin/sh
set -eu

WEB_ENV="${WEB_ENV:-}"
API_BASE_URL="${API_BASE_URL:-}"
BACKEND_PORT="${BACKEND_PORT:-}"

json_escape() {
  # Minimal JSON string escaping for config generation.
  printf '%s' "$1" | sed \
    -e 's/\\/\\\\/g' \
    -e 's/"/\\"/g' \
    -e 's/\r/\\r/g' \
    -e 's/\n/\\n/g' \
    -e 's/\t/\\t/g'
}

WEB_ENV_JSON="$(json_escape "${WEB_ENV}")"
API_BASE_URL_JSON="$(json_escape "${API_BASE_URL}")"
BACKEND_PORT_JSON="$(json_escape "${BACKEND_PORT}")"

cat > /usr/share/nginx/html/config.js <<EOF
(function () {
  const envOverride = "${WEB_ENV_JSON}";
  const apiBaseUrlOverride = "${API_BASE_URL_JSON}";
  const backendPortOverride = "${BACKEND_PORT_JSON}";

  const host = window.location.hostname;
  const protocol = window.location.protocol;

  const inferredEnv = (host === "127.0.0.1" || host === "localhost") ? "dev" : "prod";
  const env = envOverride || inferredEnv;

  const toApiHost = (h) => (h && h.startsWith("web.")) ? ("api." + h.slice(4)) : h;
  const apiHost = toApiHost(host);
  const defaultApiBaseUrl = protocol + "//" + apiHost;
  const defaultDockerBaseUrl = backendPortOverride ? (protocol + "//" + host + ":" + backendPortOverride) : defaultApiBaseUrl;

  const apiBaseUrlMap = {
    dev: "http://127.0.0.1:8080",
    test: defaultDockerBaseUrl,
    prod: defaultApiBaseUrl
  };

  const apiBaseUrl = apiBaseUrlOverride ? apiBaseUrlOverride : (apiBaseUrlMap[env] || apiBaseUrlMap.prod);

  window.__APP_CONFIG__ = {
    env,
    apiBaseUrlMap,
    apiBaseUrl
  };
})();
EOF

echo "[web] generated /config.js (WEB_ENV=${WEB_ENV:-auto})"
