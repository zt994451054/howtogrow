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

if command -v "${PROJECT_DIR}/mvnw" >/dev/null 2>&1; then
  MVN="${PROJECT_DIR}/mvnw"
elif command -v mvn >/dev/null 2>&1; then
  MVN="mvn"
else
  echo "mvn not found (need mvn or ./mvnw to build backend jar locally)" >&2
  exit 1
fi

echo "Building jar with ${MVN} (skip tests)..."
(
  cd "${PROJECT_DIR}"
  "${MVN}" -DskipTests package
)

JAR_PATH="$(
  ls -1 "${PROJECT_DIR}"/target/*.jar 2>/dev/null \
    | grep -vE '\.original$' \
    | head -n 1 \
    || true
)"
if [ -z "${JAR_PATH}" ]; then
  echo "jar not found under target/*.jar (maven build might have failed)" >&2
  exit 1
fi

# Keep the build context small and deterministic: pass a relative JAR_FILE path.
JAR_FILE_REL="target/$(basename "${JAR_PATH}")"

PLATFORM="${PLATFORM:-linux/amd64}"

echo "Building docker image: ${IMAGE_REF}"
if docker buildx version >/dev/null 2>&1; then
  docker buildx build \
    --platform "${PLATFORM}" \
    --build-arg "JAR_FILE=${JAR_FILE_REL}" \
    -t "${IMAGE_REF}" \
    --load \
    "${PROJECT_DIR}"
else
  # Fallback: builds for the local architecture only.
  docker build --build-arg "JAR_FILE=${JAR_FILE_REL}" -t "${IMAGE_REF}" "${PROJECT_DIR}"
fi

echo "Built: ${IMAGE_REF}"
