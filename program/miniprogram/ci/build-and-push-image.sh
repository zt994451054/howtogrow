#!/usr/bin/env bash
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_DIR="$(cd "${SCRIPT_DIR}/.." && pwd)"

REGISTRY="${REGISTRY:-}"
NAMESPACE="${NAMESPACE:-}"
IMAGE_NAME="${IMAGE_NAME:-howtogrow-miniprogram}"
IMAGE_TAG="${IMAGE_TAG:-$(git -C "${PROJECT_DIR}" rev-parse --short HEAD 2>/dev/null || date +%Y%m%d%H%M%S)}"

if ! command -v docker >/dev/null 2>&1; then
  echo "docker not found" >&2
  exit 1
fi

if [ -z "${REGISTRY}" ] || [ -z "${NAMESPACE}" ]; then
  echo "Missing env: REGISTRY and NAMESPACE (e.g. REGISTRY=registry.example.com NAMESPACE=howtogrow)" >&2
  exit 1
fi

IMAGE_REF="${REGISTRY}/${NAMESPACE}/${IMAGE_NAME}:${IMAGE_TAG}"

echo "Building image (artifact only): ${IMAGE_REF}"
docker build -t "${IMAGE_REF}" "${PROJECT_DIR}"

echo "Pushing image: ${IMAGE_REF}"
docker push "${IMAGE_REF}"

