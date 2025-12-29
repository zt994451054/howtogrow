#!/usr/bin/env bash
set -euo pipefail

BASE_URL="${BASE_URL:-http://127.0.0.1:8080}"

if ! command -v curl >/dev/null 2>&1; then
  echo "curl not found" >&2
  exit 1
fi

echo "== Health =="
curl -fsS "${BASE_URL}/actuator/health" | cat
echo

echo "== Admin login (admin/admin) =="
ADMIN_TOKEN="$(
  curl -fsS "${BASE_URL}/api/v1/admin/auth/login" \
    -H 'Content-Type: application/json' \
    -d '{"username":"admin","password":"admin"}' \
  | sed -n 's/.*"token":"\\([^"]*\\)".*/\\1/p'
)"
if [ -z "${ADMIN_TOKEN}" ]; then
  echo "failed to parse admin token" >&2
  exit 1
fi
echo "admin token ok"

echo "== Admin me =="
curl -fsS "${BASE_URL}/api/v1/admin/auth/me" -H "Authorization: Bearer ${ADMIN_TOKEN}" | cat
echo

echo "== Miniprogram login (mock) =="
MP_TOKEN="$(
  curl -fsS "${BASE_URL}/api/v1/miniprogram/auth/wechat-login" \
    -H 'Content-Type: application/json' \
    -d '{"code":"mock:smoke_user"}' \
  | sed -n 's/.*"token":"\\([^"]*\\)".*/\\1/p'
)"
if [ -z "${MP_TOKEN}" ]; then
  echo "failed to parse miniprogram token" >&2
  exit 1
fi
echo "miniprogram token ok"

echo "== Miniprogram me =="
curl -fsS "${BASE_URL}/api/v1/miniprogram/me" -H "Authorization: Bearer ${MP_TOKEN}" | cat
echo

echo "== Notes =="
echo "- AI 对话/AI 总结需要订阅（user_account.subscription_end_at > now），否则会返回 SUBSCRIPTION_REQUIRED"
echo "- 如需验证 SSE：先创建 session -> 发消息 -> 调用 stream"

