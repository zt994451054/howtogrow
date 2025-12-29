#!/usr/bin/env bash
set -euo pipefail

# 为本地开发提供稳定的环境变量（尤其是 JWT 密钥），避免每次重启都让客户端 token 失效。
# 该脚本不会把密钥写入仓库文件，而是写到用户目录下的私有文件中。

ENV_FILE="${HOWTOTALK_DEV_ENV_FILE:-$HOME/.howtogrow/howtotalk-dev.env}"

ensure_env_file() {
  if [ -f "$ENV_FILE" ]; then
    return 0
  fi

  mkdir -p "$(dirname "$ENV_FILE")"

  local mp_secret
  local admin_secret
  mp_secret="$(python3 -c 'import secrets; print(secrets.token_hex(32))')"
  admin_secret="$(python3 -c 'import secrets; print(secrets.token_hex(32))')"

  cat >"$ENV_FILE" <<EOF
# Auto-generated at $(date -u +"%Y-%m-%dT%H:%M:%SZ")
# WARNING: keep this file private. Do NOT commit to git.
export JWT_SECRET_MINIPROGRAM='${mp_secret}'
export JWT_SECRET_ADMIN='${admin_secret}'
EOF

  chmod 600 "$ENV_FILE" || true
}

ensure_env_file

# shellcheck disable=SC1090
source "$ENV_FILE"

if [ -z "${JWT_SECRET_MINIPROGRAM:-}" ] || [ -z "${JWT_SECRET_ADMIN:-}" ]; then
  echo "missing JWT secrets in $ENV_FILE" >&2
  exit 1
fi

echo "Loaded stable JWT secrets from: $ENV_FILE"
echo "To reuse in your shell (zsh/bash): source \"$ENV_FILE\""
