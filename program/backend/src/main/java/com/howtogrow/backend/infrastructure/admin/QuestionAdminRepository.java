package com.howtogrow.backend.infrastructure.admin;

import java.util.List;
import java.util.Map;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

@Repository
public class QuestionAdminRepository {
  private final NamedParameterJdbcTemplate jdbc;

  public QuestionAdminRepository(NamedParameterJdbcTemplate jdbc) {
    this.jdbc = jdbc;
  }

  public long insertQuestion(String content, int minAge, int maxAge, String questionType, int status) {
    KeyHolder kh = new GeneratedKeyHolder();
    var sql =
        """
        INSERT INTO question(content, min_age, max_age, question_type, status, created_at, updated_at)
        VALUES (:content, :minAge, :maxAge, :questionType, :status, NOW(3), NOW(3))
        """;
    jdbc.update(
        sql,
        new org.springframework.jdbc.core.namedparam.MapSqlParameterSource()
            .addValue("content", content)
            .addValue("minAge", minAge)
            .addValue("maxAge", maxAge)
            .addValue("questionType", questionType)
            .addValue("status", status),
        kh);
    var id = kh.getKey();
    if (id == null) {
      throw new IllegalStateException("failed to insert question");
    }
    return id.longValue();
  }

  public boolean existsSameQuestion(int minAge, int maxAge, String questionType, String content) {
    var sql =
        """
        SELECT COUNT(*)
        FROM question
        WHERE min_age = :minAge
          AND max_age = :maxAge
          AND question_type = :questionType
          AND deleted_at IS NULL
          AND content = :content
        """;
    Integer count =
        jdbc.queryForObject(
            sql,
            Map.of("minAge", minAge, "maxAge", maxAge, "questionType", questionType, "content", content),
            Integer.class);
    return count != null && count > 0;
  }

  public void softDeleteQuestion(long questionId) {
    jdbc.update(
        "UPDATE question SET status = 0, deleted_at = NOW(3), updated_at = NOW(3) WHERE id = :id AND deleted_at IS NULL",
        Map.of("id", questionId));
  }

  public void softDeleteQuestions(List<Long> questionIds) {
    if (questionIds == null || questionIds.isEmpty()) {
      return;
    }
    jdbc.update(
        "UPDATE question SET status = 0, deleted_at = NOW(3), updated_at = NOW(3) WHERE id IN (:ids) AND deleted_at IS NULL",
        Map.of("ids", questionIds));
  }

  public List<Long> listOptionIdsByQuestion(long questionId) {
    var sql =
        """
        SELECT id
        FROM question_option
        WHERE question_id = :questionId AND deleted_at IS NULL
        """;
    return jdbc.queryForList(sql, Map.of("questionId", questionId), Long.class);
  }

  public List<Long> listOptionIdsByQuestions(List<Long> questionIds) {
    if (questionIds == null || questionIds.isEmpty()) {
      return List.of();
    }
    var sql =
        """
        SELECT id
        FROM question_option
        WHERE question_id IN (:questionIds) AND deleted_at IS NULL
        """;
    return jdbc.queryForList(sql, Map.of("questionIds", questionIds), Long.class);
  }

  public void deleteOptionDimensionScores(List<Long> optionIds) {
    if (optionIds == null || optionIds.isEmpty()) {
      return;
    }
    jdbc.update("DELETE FROM option_dimension_score WHERE option_id IN (:ids)", Map.of("ids", optionIds));
  }

  public void softDeleteOptions(long questionId) {
    jdbc.update(
        "UPDATE question_option SET deleted_at = NOW(3), updated_at = NOW(3) WHERE question_id = :questionId AND deleted_at IS NULL",
        Map.of("questionId", questionId));
  }

  public void softDeleteOptionsByQuestionIds(List<Long> questionIds) {
    if (questionIds == null || questionIds.isEmpty()) {
      return;
    }
    jdbc.update(
        "UPDATE question_option SET deleted_at = NOW(3), updated_at = NOW(3) WHERE question_id IN (:questionIds) AND deleted_at IS NULL",
        Map.of("questionIds", questionIds));
  }

  public void updateQuestion(long questionId, String content, int minAge, int maxAge, String questionType, int status) {
    var sql =
        """
        UPDATE question
        SET content = :content,
            min_age = :minAge,
            max_age = :maxAge,
            question_type = :questionType,
            status = :status,
            updated_at = NOW(3)
        WHERE id = :id AND deleted_at IS NULL
        """;
    jdbc.update(
        sql,
        Map.of(
            "id", questionId,
            "content", content,
            "minAge", minAge,
            "maxAge", maxAge,
            "questionType", questionType,
            "status", status));
  }

  public long insertOption(
      long questionId, String content, int suggestFlag, String improvementTip, int sortNo) {
    KeyHolder kh = new GeneratedKeyHolder();
    var sql =
        """
        INSERT INTO question_option(question_id, content, suggest_flag, improvement_tip, sort_no, created_at, updated_at)
        VALUES (:questionId, :content, :suggestFlag, :improvementTip, :sortNo, NOW(3), NOW(3))
        """;
    jdbc.update(
        sql,
        new org.springframework.jdbc.core.namedparam.MapSqlParameterSource()
            .addValue("questionId", questionId)
            .addValue("content", content)
            .addValue("suggestFlag", suggestFlag)
            .addValue("improvementTip", improvementTip)
            .addValue("sortNo", sortNo),
        kh);
    var id = kh.getKey();
    if (id == null) {
      throw new IllegalStateException("failed to insert question_option");
    }
    return id.longValue();
  }

  public void insertOptionDimensionScore(long optionId, String dimensionCode, int score) {
    var sql =
        """
        INSERT INTO option_dimension_score(option_id, dimension_code, score, created_at, updated_at)
        VALUES (:optionId, :dimensionCode, :score, NOW(3), NOW(3))
        """;
    jdbc.update(sql, Map.of("optionId", optionId, "dimensionCode", dimensionCode, "score", score));
  }

  public void replaceQuestionTroubleScenes(long questionId, List<Long> sceneIds) {
    jdbc.update("DELETE FROM question_trouble_scene WHERE question_id = :qid", Map.of("qid", questionId));
    if (sceneIds == null || sceneIds.isEmpty()) {
      return;
    }
    for (var sid : sceneIds) {
      if (sid == null || sid <= 0) {
        continue;
      }
      jdbc.update(
          """
          INSERT INTO question_trouble_scene(question_id, scene_id, created_at)
          VALUES (:qid, :sid, NOW(3))
          """,
          Map.of("qid", questionId, "sid", sid));
    }
  }

  public List<Long> listTroubleSceneIdsByQuestion(long questionId) {
    var sql =
        """
        SELECT scene_id
        FROM question_trouble_scene
        WHERE question_id = :qid
        ORDER BY id ASC
        """;
    return jdbc.queryForList(sql, Map.of("qid", questionId), Long.class);
  }

  public void deleteQuestionTroubleScenesBySceneId(long sceneId) {
    jdbc.update("DELETE FROM question_trouble_scene WHERE scene_id = :sid", Map.of("sid", sceneId));
  }

  public void deleteQuestionTroubleScenesBySceneIds(List<Long> sceneIds) {
    if (sceneIds == null || sceneIds.isEmpty()) {
      return;
    }
    jdbc.update("DELETE FROM question_trouble_scene WHERE scene_id IN (:sids)", Map.of("sids", sceneIds));
  }

  public void deleteQuestionTroubleScenesByQuestionIds(List<Long> questionIds) {
    if (questionIds == null || questionIds.isEmpty()) {
      return;
    }
    jdbc.update("DELETE FROM question_trouble_scene WHERE question_id IN (:qids)", Map.of("qids", questionIds));
  }
}
