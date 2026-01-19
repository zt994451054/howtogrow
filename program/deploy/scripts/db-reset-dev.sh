#!/usr/bin/env bash
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_DIR="$(cd "${SCRIPT_DIR}/../.." && pwd)"

echo "Stopping dev compose..."
docker compose -f "${PROJECT_DIR}/deploy/docker-compose.dev.yml" down -v

echo "Starting dev MySQL and initializing schema..."
docker compose -f "${PROJECT_DIR}/deploy/docker-compose.dev.yml" up -d

echo "Done."
echo "Tip: If you only changed schema.sql and want a non-destructive sync, use: ${PROJECT_DIR}/deploy/scripts/db-apply-ddl-dev.sh"
