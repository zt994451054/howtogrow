#!/bin/sh
set -eu

WEB_ENV="${WEB_ENV:-dev}"
API_BASE_URL="${API_BASE_URL:-}"
BACKEND_PORT="${BACKEND_PORT:-8080}"

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
  const env = "${WEB_ENV_JSON}";
  const apiBaseUrlOverride = "${API_BASE_URL_JSON}";
  const backendPort = "${BACKEND_PORT_JSON}";

  const host = window.location.hostname;
  const protocol = window.location.protocol;
  const defaultDockerBaseUrl = protocol + "//" + host + ":" + backendPort;

  const apiBaseUrlMap = {
    dev: "http://127.0.0.1:8080",
    test: defaultDockerBaseUrl,
    prod: defaultDockerBaseUrl
  };

  const apiBaseUrl = apiBaseUrlOverride ? apiBaseUrlOverride : (apiBaseUrlMap[env] || apiBaseUrlMap.dev);

  window.__APP_CONFIG__ = {
    env,
    apiBaseUrlMap,
    apiBaseUrl
  };
})();
EOF

echo "[web] generated /config.js (WEB_ENV=${WEB_ENV})"

