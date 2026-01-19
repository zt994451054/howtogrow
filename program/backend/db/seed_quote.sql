-- MySQL 8.0
-- 随机生成一批 `quote`（鸡汤语）测试数据。
-- 注意：不包含 TRUNCATE/DELETE，避免误清空线上数据。
--
-- 这里不使用 CTE，避免不同 MySQL 版本/方言对 `WITH ... INSERT` 的兼容差异。

SET NAMES utf8mb4;

INSERT INTO `quote` (`content`, `scene`, `min_age`, `max_age`, `status`)
SELECT
  CONCAT(
    base_text,
    ' #', LPAD(n, 3, '0')
  ) AS content,
  scene,
  min_age,
  18 AS max_age,
  1 AS status
FROM (
  SELECT
    (tens.d * 10 + ones.d + 1) AS n,
    ELT(1 + FLOOR(RAND() * 4), '每日觉察','育儿状态','烦恼档案','育儿日记') AS scene,
    FLOOR(RAND() * 19) AS min_age,
    ELT(1 + FLOOR(RAND() * 10),
        '先连接，再纠正。',
        '情绪被看见，行为才会改变。',
        '允许孩子慢一点，也允许自己慢一点。',
        '别急着讲道理，先把关系抱紧。',
        '把注意力放在可控的 1% 上。',
        '把规则说清楚，也把爱说清楚。',
        '你在努力，孩子也在学习。',
        '先照顾情绪，再处理事情。',
        '小步前进，也是前进。',
        '遇到反复很正常，复盘比责备更有用。'
    ) AS base_text
  FROM (
    SELECT 0 AS d UNION ALL SELECT 1 UNION ALL SELECT 2 UNION ALL SELECT 3 UNION ALL SELECT 4
    UNION ALL SELECT 5 UNION ALL SELECT 6 UNION ALL SELECT 7 UNION ALL SELECT 8 UNION ALL SELECT 9
  ) ones
  CROSS JOIN (
    SELECT 0 AS d UNION ALL SELECT 1 UNION ALL SELECT 2 UNION ALL SELECT 3
    UNION ALL SELECT 4 UNION ALL SELECT 5 UNION ALL SELECT 6 UNION ALL SELECT 7
  ) tens
  WHERE (tens.d * 10 + ones.d) < 80
) gen;
