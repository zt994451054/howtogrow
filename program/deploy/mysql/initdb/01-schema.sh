#!/usr/bin/env sh
set -eu

if [ -z "${MYSQL_DATABASE:-}" ]; then
  echo "MYSQL_DATABASE is required" >&2
  exit 1
fi

if [ ! -f /docker-entrypoint-initdb.d/schema.sql ]; then
  echo "schema.sql not found at /docker-entrypoint-initdb.d/schema.sql" >&2
  exit 1
fi

echo "Initializing schema for database: ${MYSQL_DATABASE}"
mysql -uroot -p"${MYSQL_ROOT_PASSWORD}" "${MYSQL_DATABASE}" < /docker-entrypoint-initdb.d/schema.sql

