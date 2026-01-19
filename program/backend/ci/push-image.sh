#!/usr/bin/env bash
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_DIR="$(cd "${SCRIPT_DIR}/.." && pwd)"

REGISTRY="crpi-w1x1q83txaky0cp1.cn-hangzhou.personal.cr.aliyuncs.com"
NAMESPACE="howtotalk"
REPOSITORY="backend"

IMAGE_TAG="${1:-${IMAGE_TAG:-latest}}"

IMAGE_REF="${REGISTRY}/${NAMESPACE}/${REPOSITORY}:${IMAGE_TAG}"

if ! command -v docker >/dev/null 2>&1; then
  echo "docker not found" >&2
  exit 1
fi

# Optional: non-interactive login (do NOT echo secrets).
if [ -n "${ACR_USERNAME:-}" ] && [ -n "${ACR_PASSWORD:-}" ]; then
  printf '%s' "${ACR_PASSWORD}" | docker login "${REGISTRY}" --username "${ACR_USERNAME}" --password-stdin >/dev/null
fi

echo "Pushing docker image: ${IMAGE_REF}"
docker push "${IMAGE_REF}"
