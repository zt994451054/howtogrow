package com.howtogrow.backend.infrastructure.admin;

import com.howtogrow.backend.infrastructure.db.SqlPagination;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.stereotype.Repository;

@Repository
public class QuestionQueryRepository {
  private final NamedParameterJdbcTemplate jdbc;

  public QuestionQueryRepository(NamedParameterJdbcTemplate jdbc) {
    this.jdbc = jdbc;
  }

  public long countQuestions(
      Integer minAge, Integer maxAge, Integer status, String questionType, Long troubleSceneId, String keyword) {
    var params = new MapSqlParameterSource();
    var sql = new StringBuilder("SELECT COUNT(*) FROM question q WHERE q.deleted_at IS NULL");
    appendWhere(sql, params, minAge, maxAge, status, questionType, troubleSceneId, keyword);
    Long count = jdbc.queryForObject(sql.toString(), params, Long.class);
    return count == null ? 0L : count;
  }

  public List<QuestionSummaryRow> listQuestions(
      Integer minAge,
      Integer maxAge,
      Integer status,
      String questionType,
      Long troubleSceneId,
      String keyword,
      int offset,
      int limit) {
    var params = new MapSqlParameterSource().addValue("offset", offset).addValue("limit", limit);
    var where = new StringBuilder("WHERE q.deleted_at IS NULL");
    appendWhere(where, params, minAge, maxAge, status, questionType, troubleSceneId, keyword);
    var sql =
        """
        SELECT q.id, q.min_age, q.max_age, q.question_type, q.status, q.content
        FROM question q
        """
            + where
            + "\nORDER BY q.id DESC\n"
            + SqlPagination.limitOffset(offset, limit);
    return jdbc.query(
        sql,
        params,
        (rs, rowNum) ->
            new QuestionSummaryRow(
                rs.getLong("id"),
                rs.getInt("min_age"),
                rs.getInt("max_age"),
                rs.getString("question_type"),
                rs.getInt("status"),
                rs.getString("content")));
  }

  private static void appendWhere(
      StringBuilder sql,
      MapSqlParameterSource params,
      Integer minAge,
      Integer maxAge,
      Integer status,
      String questionType,
      Long troubleSceneId,
      String keyword) {
    if (minAge != null) {
      sql.append("\n  AND q.max_age >= :minAge");
      params.addValue("minAge", minAge);
    }
    if (maxAge != null) {
      sql.append("\n  AND q.min_age <= :maxAge");
      params.addValue("maxAge", maxAge);
    }
    if (status != null) {
      sql.append("\n  AND q.status = :status");
      params.addValue("status", status);
    }
    if (questionType != null && !questionType.trim().isBlank()) {
      sql.append("\n  AND q.question_type = :questionType");
      params.addValue("questionType", questionType.trim().toUpperCase(Locale.ROOT));
    }
    if (troubleSceneId != null) {
      sql.append(
          """

            AND EXISTS (
              SELECT 1
              FROM question_trouble_scene qts
              WHERE qts.question_id = q.id AND qts.scene_id = :troubleSceneId
            )
          """);
      params.addValue("troubleSceneId", troubleSceneId);
    }
    if (keyword != null && !keyword.trim().isBlank()) {
      sql.append("\n  AND q.content LIKE :keyword");
      params.addValue("keyword", "%" + keyword.trim() + "%");
    }
  }

  public List<QuestionDetailRow> getQuestionDetail(long questionId) {
    var sql =
        """
        SELECT
          q.id AS question_id,
          q.content AS question_content,
          q.question_type,
          q.min_age,
          q.max_age,
          o.id AS option_id,
          o.content AS option_content,
          o.suggest_flag,
          o.improvement_tip,
          o.sort_no AS option_sort_no,
          ods.dimension_code,
          ods.score AS dimension_score
        FROM question q
        JOIN question_option o ON o.question_id = q.id AND o.deleted_at IS NULL
        LEFT JOIN option_dimension_score ods ON ods.option_id = o.id
        WHERE q.id = :questionId AND q.deleted_at IS NULL
        ORDER BY o.sort_no ASC, o.id ASC, ods.dimension_code ASC
        """;
    return jdbc.query(
        sql,
        Map.of("questionId", questionId),
        (rs, rowNum) -> {
          var scoreObj = rs.getObject("dimension_score");
          Integer dimensionScore = null;
          if (scoreObj instanceof Number n) {
            dimensionScore = n.intValue();
          }

          return new QuestionDetailRow(
              rs.getLong("question_id"),
              rs.getString("question_content"),
              rs.getString("question_type"),
              rs.getInt("min_age"),
              rs.getInt("max_age"),
              rs.getLong("option_id"),
              rs.getString("option_content"),
              rs.getInt("suggest_flag"),
              rs.getString("improvement_tip"),
              rs.getInt("option_sort_no"),
              rs.getString("dimension_code"),
              dimensionScore);
        });
  }

  public Map<Long, List<Long>> mapTroubleSceneIdsByQuestionIds(List<Long> questionIds) {
    if (questionIds == null || questionIds.isEmpty()) {
      return Map.of();
    }

    var sql =
        """
        SELECT question_id, scene_id
        FROM question_trouble_scene
        WHERE question_id IN (:questionIds)
        ORDER BY id ASC
        """;
    var rows =
        jdbc.query(
            sql,
            Map.of("questionIds", questionIds),
            (rs, rowNum) -> new QuestionTroubleSceneRow(rs.getLong("question_id"), rs.getLong("scene_id")));

    var out = new HashMap<Long, List<Long>>();
    for (var row : rows) {
      out.computeIfAbsent(row.questionId(), ignored -> new ArrayList<>()).add(row.sceneId());
    }
    return out;
  }

  public record QuestionSummaryRow(long id, int minAge, int maxAge, String questionType, int status, String content) {}

  public record QuestionDetailRow(
      long questionId,
      String questionContent,
      String questionType,
      int minAge,
      int maxAge,
      long optionId,
      String optionContent,
      int suggestFlag,
      String improvementTip,
      int optionSortNo,
      String dimensionCode,
      Integer dimensionScore) {}

  public record QuestionTroubleSceneRow(long questionId, long sceneId) {}
}
