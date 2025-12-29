#!/usr/bin/env bash
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_DIR="$(cd "${SCRIPT_DIR}/../.." && pwd)"

# 生成/加载稳定的 JWT 密钥（避免服务重启导致历史 token 全部失效）
# shellcheck disable=SC1091
source "${SCRIPT_DIR}/dev-env.sh"

: "${SPRING_PROFILES_ACTIVE:=dev}"
: "${SERVER_PORT:=8080}"

cd "${PROJECT_DIR}"

if [ ! -f "backend/target/howtogrow-backend-0.0.1-SNAPSHOT.jar" ]; then
  echo "missing jar: backend/target/howtogrow-backend-0.0.1-SNAPSHOT.jar" >&2
  echo "build it first: (cd backend && mvn -DskipTests clean package)" >&2
  exit 1
fi

nohup java -jar backend/target/howtogrow-backend-0.0.1-SNAPSHOT.jar \
  --spring.profiles.active="${SPRING_PROFILES_ACTIVE}" \
  --server.port="${SERVER_PORT}" \
  > backend/run/server-8080.log 2>&1 &

echo $! > backend/run/server-8080.pid
echo "Started backend on port ${SERVER_PORT}, pid=$(cat backend/run/server-8080.pid)"
