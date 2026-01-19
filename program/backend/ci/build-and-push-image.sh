#!/usr/bin/env bash
set -euo pipefail

# Backward-compatible wrapper; prefer build-image.sh + push-image.sh.
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"

IMAGE_TAG="${1:-${IMAGE_TAG:-}}"

"${SCRIPT_DIR}/build-image.sh" "${IMAGE_TAG}"
"${SCRIPT_DIR}/push-image.sh" "${IMAGE_TAG}"
