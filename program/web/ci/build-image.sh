#!/usr/bin/env bash
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_DIR="$(cd "${SCRIPT_DIR}/.." && pwd)"

REGISTRY="crpi-w1x1q83txaky0cp1.cn-hangzhou.personal.cr.aliyuncs.com"
NAMESPACE="howtotalk"
REPOSITORY="web"

IMAGE_TAG="${1:-${IMAGE_TAG:-latest}}"

IMAGE_REF="${REGISTRY}/${NAMESPACE}/${REPOSITORY}:${IMAGE_TAG}"

if ! command -v docker >/dev/null 2>&1; then
  echo "docker not found" >&2
  exit 1
fi

PLATFORM="${PLATFORM:-linux/amd64}"

echo "Building docker image: ${IMAGE_REF}"
if docker buildx version >/dev/null 2>&1; then
  docker buildx build --platform "${PLATFORM}" -t "${IMAGE_REF}" --load "${PROJECT_DIR}"
else
  # Fallback: builds for the local architecture only.
  docker build -t "${IMAGE_REF}" "${PROJECT_DIR}"
fi

echo "Built: ${IMAGE_REF}"
