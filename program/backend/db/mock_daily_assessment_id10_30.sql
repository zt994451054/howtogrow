-- 用途：基于 daily_assessment.id=10 复制整套“每日自测”链路数据（含子表），并将 submitted_at 按天依次往前回填，生成 30 份 mock 数据。
-- 适用：MySQL 8.0
-- 注意：脚本不会修改源数据（id=10），而是额外插入 30 条新的 daily_assessment 记录及其关联数据。

SET NAMES utf8mb4;

DELIMITER $$

DROP PROCEDURE IF EXISTS sp_mock_daily_assessment_from_id $$
CREATE PROCEDURE sp_mock_daily_assessment_from_id(
  IN p_source_assessment_id BIGINT UNSIGNED,
  IN p_copies INT
)
BEGIN
  DECLARE v_i INT DEFAULT 1;
  DECLARE v_user_id BIGINT UNSIGNED;
  DECLARE v_child_id BIGINT UNSIGNED;
  DECLARE v_submitted_at DATETIME(3);
  DECLARE v_new_assessment_id BIGINT UNSIGNED;
  DECLARE v_new_submitted_at DATETIME(3);

  SELECT user_id, child_id, submitted_at
    INTO v_user_id, v_child_id, v_submitted_at
  FROM daily_assessment
  WHERE id = p_source_assessment_id;

  IF v_user_id IS NULL THEN
    SIGNAL SQLSTATE '45000'
      SET MESSAGE_TEXT = 'source daily_assessment not found';
  END IF;

  START TRANSACTION;
  WHILE v_i <= p_copies DO
    SET v_new_submitted_at = v_submitted_at - INTERVAL v_i DAY;

    INSERT INTO daily_assessment (user_id, child_id, submitted_at, created_at, updated_at)
    VALUES (v_user_id, v_child_id, v_new_submitted_at, v_new_submitted_at, v_new_submitted_at);
    SET v_new_assessment_id = LAST_INSERT_ID();

    -- 题目明细：按 question_id 复制（同一次 assessment 内 question_id 唯一，后续用于映射 answer 的新 item_id）
    INSERT INTO daily_assessment_item (assessment_id, question_id, display_order, created_at, updated_at)
    SELECT v_new_assessment_id, question_id, display_order, v_new_submitted_at, v_new_submitted_at
    FROM daily_assessment_item
    WHERE assessment_id = p_source_assessment_id;

    -- 作答：通过 question_id 映射到新插入的 assessment_item_id
    INSERT INTO daily_assessment_answer (assessment_id, assessment_item_id, option_id, created_at)
    SELECT
      v_new_assessment_id,
      new_item.id,
      old_ans.option_id,
      v_new_submitted_at
    FROM daily_assessment_answer old_ans
    JOIN daily_assessment_item old_item
      ON old_ans.assessment_item_id = old_item.id
    JOIN daily_assessment_item new_item
      ON new_item.assessment_id = v_new_assessment_id
     AND new_item.question_id = old_item.question_id
    WHERE old_ans.assessment_id = p_source_assessment_id;

    -- 维度得分：通过 (question_id + option_id) 映射到新插入的 assessment_answer_id
    INSERT INTO daily_assessment_dimension_score (
      assessment_id,
      assessment_answer_id,
      dimension_code,
      score,
      created_at
    )
    SELECT
      v_new_assessment_id,
      new_ans.id,
      old_ds.dimension_code,
      old_ds.score,
      v_new_submitted_at
    FROM daily_assessment_dimension_score old_ds
    JOIN daily_assessment_answer old_ans
      ON old_ds.assessment_answer_id = old_ans.id
    JOIN daily_assessment_item old_item
      ON old_ans.assessment_item_id = old_item.id
    JOIN daily_assessment_item new_item
      ON new_item.assessment_id = v_new_assessment_id
     AND new_item.question_id = old_item.question_id
    JOIN daily_assessment_answer new_ans
      ON new_ans.assessment_id = v_new_assessment_id
     AND new_ans.assessment_item_id = new_item.id
     AND new_ans.option_id = old_ans.option_id
    WHERE old_ds.assessment_id = p_source_assessment_id;

    -- AI 总结：如果源数据存在则一并复制（0..1）
    INSERT INTO ai_assessment_summary (assessment_id, user_id, content, created_at)
    SELECT v_new_assessment_id, user_id, content, v_new_submitted_at
    FROM ai_assessment_summary
    WHERE assessment_id = p_source_assessment_id;

    SET v_i = v_i + 1;
  END WHILE;
  COMMIT;
END $$

DELIMITER ;

-- 默认按需求：基于 id=10 复制 30 份
CALL sp_mock_daily_assessment_from_id(10, 30);

