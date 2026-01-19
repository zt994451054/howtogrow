-- 用途：对基于 daily_assessment.id=10 mock 出来的“近 30 天”自测数据，调整 daily_assessment_dimension_score.score，
--      让按天聚合的分数呈现可视的趋势变化（整体随日期接近 source day 而上升，并带少量维度差异与抖动）。
-- 适用：MySQL 8.0
-- 安全：仅更新与 source(assessment_id=10) 同 user/child、且题目集合一致、且提交时间落在 (source_day-30, source_day) 的记录。

SET NAMES utf8mb4;

DELIMITER $$

DROP PROCEDURE IF EXISTS sp_adjust_daily_assessment_dimension_score_trend $$
CREATE PROCEDURE sp_adjust_daily_assessment_dimension_score_trend(
  IN p_source_assessment_id BIGINT UNSIGNED,
  IN p_days INT,
  IN p_min_mult DECIMAL(10,4),
  IN p_max_mult DECIMAL(10,4)
)
BEGIN
  DECLARE v_user_id BIGINT UNSIGNED;
  DECLARE v_child_id BIGINT UNSIGNED;
  DECLARE v_submitted_at DATETIME(3);
  DECLARE v_src_item_cnt INT;
  DECLARE v_src_item_hash_sum BIGINT;

  IF p_days IS NULL OR p_days < 2 THEN
    SIGNAL SQLSTATE '45000'
      SET MESSAGE_TEXT = 'p_days must be >= 2';
  END IF;

  IF p_min_mult IS NULL OR p_max_mult IS NULL OR p_min_mult <= 0 OR p_max_mult <= 0 OR p_min_mult >= p_max_mult THEN
    SIGNAL SQLSTATE '45000'
      SET MESSAGE_TEXT = 'invalid multiplier range';
  END IF;

  SELECT user_id, child_id, submitted_at
    INTO v_user_id, v_child_id, v_submitted_at
  FROM daily_assessment
  WHERE id = p_source_assessment_id;

  IF v_user_id IS NULL THEN
    SIGNAL SQLSTATE '45000'
      SET MESSAGE_TEXT = 'source daily_assessment not found';
  END IF;

  SELECT COUNT(*), COALESCE(SUM(CRC32(CAST(question_id AS CHAR))), 0)
    INTO v_src_item_cnt, v_src_item_hash_sum
  FROM daily_assessment_item
  WHERE assessment_id = p_source_assessment_id;

  IF v_src_item_cnt = 0 THEN
    SIGNAL SQLSTATE '45000'
      SET MESSAGE_TEXT = 'source assessment has no items';
  END IF;

  START TRANSACTION;
  UPDATE daily_assessment_dimension_score ds
  JOIN daily_assessment a
    ON a.id = ds.assessment_id
  JOIN daily_assessment_answer ans
    ON ans.id = ds.assessment_answer_id
  JOIN option_dimension_score ods
    ON ods.option_id = ans.option_id
   AND ods.dimension_code = ds.dimension_code
  JOIN (
    SELECT a2.id AS assessment_id,
           (p_days - DATEDIFF(DATE(v_submitted_at), DATE(a2.submitted_at))) / (p_days - 1) AS progress_norm
    FROM daily_assessment a2
    JOIN (
      SELECT assessment_id
      FROM daily_assessment_item
      GROUP BY assessment_id
      HAVING COUNT(*) = v_src_item_cnt
         AND SUM(CRC32(CAST(question_id AS CHAR))) = v_src_item_hash_sum
    ) sig
      ON sig.assessment_id = a2.id
    WHERE a2.user_id = v_user_id
      AND a2.child_id = v_child_id
      AND a2.id <> p_source_assessment_id
      AND a2.submitted_at >= v_submitted_at - INTERVAL p_days DAY
      AND a2.submitted_at < v_submitted_at
  ) targets
    ON targets.assessment_id = a.id
  SET ds.score = LEAST(
    99,
    GREATEST(
      1,
      ROUND(
        ods.score * (
          (p_min_mult + (p_max_mult - p_min_mult) * targets.progress_norm)
          + (CASE WHEN MOD(CRC32(ds.dimension_code), 2) = 0 THEN 1 ELSE -1 END) * 0.08 * targets.progress_norm
          + ((CAST(MOD(CRC32(CONCAT(a.id, ':', ds.dimension_code)), 5) AS SIGNED) - 2) * 0.01)
        )
      )
    )
  );
  COMMIT;
END $$

DELIMITER ;

-- 默认按需求：对基于 id=10 的 30 天 mock 数据做趋势调整（0.70x -> 1.40x）
CALL sp_adjust_daily_assessment_dimension_score_trend(10, 30, 0.70, 1.40);

-- 校验示例（趋势查看：按天 + 维度聚合）
-- SELECT DATE(a.submitted_at) day, ds.dimension_code, SUM(ds.score) sum_score
-- FROM daily_assessment_dimension_score ds
-- JOIN daily_assessment a ON a.id = ds.assessment_id
-- WHERE a.user_id = (SELECT user_id FROM daily_assessment WHERE id=10)
--   AND a.child_id = (SELECT child_id FROM daily_assessment WHERE id=10)
--   AND a.submitted_at >= (SELECT submitted_at FROM daily_assessment WHERE id=10) - INTERVAL 30 DAY
--   AND a.submitted_at <= (SELECT submitted_at FROM daily_assessment WHERE id=10)
-- GROUP BY day, ds.dimension_code
-- ORDER BY day, ds.dimension_code;
