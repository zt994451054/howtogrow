-- MySQL 8.0
-- Patch：为 child.parent_identity 增加可选值「其他监护人」
--
-- 背景：schema.sql 使用 CREATE TABLE IF NOT EXISTS，不会更新已存在表的 CHECK 约束。
-- 若线上/本地已建表，需要执行本脚本更新 ck_child_parent_identity。

SET NAMES utf8mb4;

SET @drop_sql =
  IF(
    EXISTS(
      SELECT 1
      FROM information_schema.table_constraints
      WHERE constraint_schema = DATABASE()
        AND table_name = 'child'
        AND constraint_name = 'ck_child_parent_identity'
        AND constraint_type = 'CHECK'
    ),
    'ALTER TABLE `child` DROP CHECK `ck_child_parent_identity`',
    'SELECT 1'
  );

PREPARE stmt_drop FROM @drop_sql;
EXECUTE stmt_drop;
DEALLOCATE PREPARE stmt_drop;

ALTER TABLE `child`
  ADD CONSTRAINT `ck_child_parent_identity`
  CHECK (`parent_identity` IN ('爸爸','妈妈','奶奶','爷爷','外公','外婆','其他监护人'));

