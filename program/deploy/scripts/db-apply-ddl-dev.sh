#!/usr/bin/env bash
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_DIR="$(cd "${SCRIPT_DIR}/../.." && pwd)"

MYSQL_DATABASE="${MYSQL_DATABASE:-howtotalk}"
MYSQL_USER="${MYSQL_USER:-root}"
MYSQL_ROOT_PASSWORD="${MYSQL_ROOT_PASSWORD:-123456}"

# When adding new NOT NULL columns to existing tables, old rows need a value.
# Override if needed for your dev data.
CHILD_PARENT_IDENTITY_DEFAULT="${CHILD_PARENT_IDENTITY_DEFAULT:-妈妈}"
QUOTE_SCENE_DEFAULT="${QUOTE_SCENE_DEFAULT:-每日觉察}"
QUOTE_MIN_AGE_DEFAULT="${QUOTE_MIN_AGE_DEFAULT:-0}"
QUOTE_MAX_AGE_DEFAULT="${QUOTE_MAX_AGE_DEFAULT:-18}"

MYSQL_CONTAINER="${MYSQL_CONTAINER:-}"
if [ -z "${MYSQL_CONTAINER}" ]; then
  MYSQL_CONTAINER="$(docker compose -f "${PROJECT_DIR}/deploy/docker-compose.dev.yml" ps -q mysql 2>/dev/null || true)"
fi

if [ -z "${MYSQL_CONTAINER}" ]; then
  echo "No MySQL container found." >&2
  echo "Set MYSQL_CONTAINER=<container_name_or_id>, or start dev compose: docker compose -f deploy/docker-compose.dev.yml up -d" >&2
  exit 1
fi

echo "Applying schema.sql to database '${MYSQL_DATABASE}' in container '${MYSQL_CONTAINER}'..."
docker exec -i \
  -e MYSQL_PWD="${MYSQL_ROOT_PASSWORD}" \
  "${MYSQL_CONTAINER}" \
  mysql --default-character-set=utf8mb4 -u"${MYSQL_USER}" "${MYSQL_DATABASE}" < "${PROJECT_DIR}/backend/db/schema.sql"

echo "Applying incremental ALTERs (idempotent)..."
docker exec -i \
  -e MYSQL_PWD="${MYSQL_ROOT_PASSWORD}" \
  "${MYSQL_CONTAINER}" \
  mysql --default-character-set=utf8mb4 -u"${MYSQL_USER}" -D "${MYSQL_DATABASE}" <<SQL
SET NAMES utf8mb4;
-- user_account.birth_date
SET @user_birth_exists := (
  SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS
  WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME='user_account' AND COLUMN_NAME='birth_date'
);
SET @sql := IF(@user_birth_exists=0,
  'ALTER TABLE user_account ADD COLUMN birth_date DATE NULL COMMENT ''出生日期（可选）'' AFTER avatar_url;',
  'SELECT ''user_account.birth_date exists'' AS info;'
);
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- child.parent_identity
SET @col_exists := (
  SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS
  WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME='child' AND COLUMN_NAME='parent_identity'
);
SET @sql := IF(@col_exists=0,
  'ALTER TABLE child ADD COLUMN parent_identity VARCHAR(16) NULL COMMENT ''家长身份：爸爸/妈妈/奶奶/爷爷/外公/外婆'' AFTER birth_date;',
  'SELECT ''child.parent_identity exists'' AS info;'
);
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @sql := CONCAT(
  'UPDATE child SET parent_identity=', QUOTE('${CHILD_PARENT_IDENTITY_DEFAULT}'),
  ' WHERE parent_identity IS NULL;'
);
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

ALTER TABLE child MODIFY COLUMN parent_identity VARCHAR(16) NOT NULL COMMENT '家长身份：爸爸/妈妈/奶奶/爷爷/外公/外婆';

-- Try to repair legacy mojibake values (wrong client charset when writing to MySQL).
UPDATE child
SET parent_identity = CONVERT(CAST(CONVERT(parent_identity USING latin1) AS BINARY) USING utf8mb4)
WHERE parent_identity NOT IN ('爸爸','妈妈','奶奶','爷爷','外公','外婆')
  AND CONVERT(CAST(CONVERT(parent_identity USING latin1) AS BINARY) USING utf8mb4) IN ('爸爸','妈妈','奶奶','爷爷','外公','外婆');

-- Recreate constraint to avoid drift.
SET @ck_exists := (
  SELECT COUNT(*) FROM INFORMATION_SCHEMA.TABLE_CONSTRAINTS
  WHERE CONSTRAINT_SCHEMA = DATABASE() AND TABLE_NAME='child' AND CONSTRAINT_NAME='ck_child_parent_identity'
);
SET @sql := IF(@ck_exists>0,
  'ALTER TABLE child DROP CHECK ck_child_parent_identity;',
  'SELECT ''ck_child_parent_identity missing'' AS info;'
);
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;
ALTER TABLE child ADD CONSTRAINT ck_child_parent_identity CHECK (parent_identity IN ('爸爸','妈妈','奶奶','爷爷','外公','外婆'));

-- quote.scene/min_age/max_age
SET @scene_exists := (
  SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS
  WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME='quote' AND COLUMN_NAME='scene'
);
SET @min_exists := (
  SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS
  WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME='quote' AND COLUMN_NAME='min_age'
);
SET @max_exists := (
  SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS
  WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME='quote' AND COLUMN_NAME='max_age'
);

SET @sql := IF(@scene_exists=0,
  CONCAT(
    'ALTER TABLE quote ADD COLUMN scene VARCHAR(16) NOT NULL DEFAULT ',
    QUOTE('${QUOTE_SCENE_DEFAULT}'),
    ' COMMENT ''场景：每日觉察/育儿状态/烦恼档案/育儿日记'' AFTER content;'
  ),
  'SELECT ''quote.scene exists'' AS info;'
);
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @sql := IF(@min_exists=0,
  CONCAT(
    'ALTER TABLE quote ADD COLUMN min_age INT NOT NULL DEFAULT ',
    '${QUOTE_MIN_AGE_DEFAULT}',
    ' COMMENT ''适用最小年龄（整数，单位：岁，含边界）'' AFTER scene;'
  ),
  'SELECT ''quote.min_age exists'' AS info;'
);
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @sql := IF(@max_exists=0,
  CONCAT(
    'ALTER TABLE quote ADD COLUMN max_age INT NOT NULL DEFAULT ',
    '${QUOTE_MAX_AGE_DEFAULT}',
    ' COMMENT ''适用最大年龄（整数，单位：岁，含边界）'' AFTER min_age;'
  ),
  'SELECT ''quote.max_age exists'' AS info;'
);
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- idx_quote_scene_age
SET @idx_exists := (
  SELECT COUNT(*) FROM INFORMATION_SCHEMA.STATISTICS
  WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME='quote' AND INDEX_NAME='idx_quote_scene_age'
);
SET @sql := IF(@idx_exists=0,
  'CREATE INDEX idx_quote_scene_age ON quote(scene, min_age, max_age);',
  'SELECT ''idx_quote_scene_age exists'' AS info;'
);
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- quote checks (optional but matches schema.sql)
-- Recreate constraints to avoid drift between old DDL and current allowed values.
SET @ck_scene_exists := (
  SELECT COUNT(*) FROM INFORMATION_SCHEMA.TABLE_CONSTRAINTS
  WHERE CONSTRAINT_SCHEMA = DATABASE() AND TABLE_NAME='quote' AND CONSTRAINT_NAME='ck_quote_scene'
);
SET @sql := IF(@ck_scene_exists>0,
  'ALTER TABLE quote DROP CHECK ck_quote_scene;',
  'SELECT ''ck_quote_scene missing'' AS info;'
);
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- Try to repair legacy mojibake values (wrong client charset when writing to MySQL).
UPDATE quote
SET scene = CONVERT(CAST(CONVERT(scene USING latin1) AS BINARY) USING utf8mb4)
WHERE scene NOT IN ('每日觉察','育儿状态','烦恼档案','育儿日记')
  AND CONVERT(CAST(CONVERT(scene USING latin1) AS BINARY) USING utf8mb4) IN ('每日觉察','育儿状态','烦恼档案','育儿日记');

ALTER TABLE quote ADD CONSTRAINT ck_quote_scene CHECK (scene IN ('每日觉察','育儿状态','烦恼档案','育儿日记'));

SET @ck_age_exists := (
  SELECT COUNT(*) FROM INFORMATION_SCHEMA.TABLE_CONSTRAINTS
  WHERE CONSTRAINT_SCHEMA = DATABASE() AND TABLE_NAME='quote' AND CONSTRAINT_NAME='ck_quote_age_range'
);
SET @sql := IF(@ck_age_exists>0,
  'ALTER TABLE quote DROP CHECK ck_quote_age_range;',
  'SELECT ''ck_quote_age_range missing'' AS info;'
);
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;
ALTER TABLE quote ADD CONSTRAINT ck_quote_age_range CHECK (min_age >= 0 AND max_age <= 18 AND min_age <= max_age);
SQL

echo "Done."
