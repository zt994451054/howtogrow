package com.howtogrow.backend.infrastructure.admin;

import java.util.List;
import java.util.Map;
import com.howtogrow.backend.infrastructure.db.SqlPagination;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class QuestionQueryRepository {
  private final NamedParameterJdbcTemplate jdbc;

  public QuestionQueryRepository(NamedParameterJdbcTemplate jdbc) {
    this.jdbc = jdbc;
  }

  public long countQuestions(Integer ageYear) {
    var where = new StringBuilder("WHERE deleted_at IS NULL");
    var params = new java.util.HashMap<String, Object>();
    if (ageYear != null) {
      where.append(" AND min_age <= :ageYear AND max_age >= :ageYear");
      params.put("ageYear", ageYear);
    }
    var sql = "SELECT COUNT(*) FROM question " + where;
    Long count = jdbc.queryForObject(sql, params, Long.class);
    return count == null ? 0L : count;
  }

  public List<QuestionSummaryRow> listQuestions(Integer ageYear, int offset, int limit) {
    var where = new StringBuilder("WHERE q.deleted_at IS NULL");
    var params = new java.util.HashMap<String, Object>();
    if (ageYear != null) {
      where.append(" AND q.min_age <= :ageYear AND q.max_age >= :ageYear");
      params.put("ageYear", ageYear);
    }
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
}
