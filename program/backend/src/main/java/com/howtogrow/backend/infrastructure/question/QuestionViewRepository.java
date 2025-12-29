package com.howtogrow.backend.infrastructure.question;

import java.util.List;
import java.util.Map;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class QuestionViewRepository {
  private final NamedParameterJdbcTemplate jdbc;

  public QuestionViewRepository(NamedParameterJdbcTemplate jdbc) {
    this.jdbc = jdbc;
  }

  public List<QuestionOptionRow> listActiveQuestionOptionRows(List<Long> questionIds) {
    if (questionIds == null || questionIds.isEmpty()) {
      return List.of();
    }
    var sql =
        """
        SELECT
          q.id AS question_id,
          q.content AS question_content,
          q.question_type,
          o.id AS option_id,
          o.content AS option_content,
          o.sort_no AS option_sort_no
        FROM question q
        JOIN question_option o ON o.question_id = q.id AND o.deleted_at IS NULL
        WHERE q.id IN (:questionIds)
          AND q.deleted_at IS NULL
          AND q.status = 1
        ORDER BY q.id ASC, o.sort_no ASC, o.id ASC
        """;
    return jdbc.query(
        sql,
        Map.of("questionIds", questionIds),
        (rs, rowNum) ->
            new QuestionOptionRow(
                rs.getLong("question_id"),
                rs.getString("question_content"),
                rs.getString("question_type"),
                rs.getLong("option_id"),
                rs.getString("option_content"),
                rs.getInt("option_sort_no")));
  }

  public record QuestionOptionRow(
      long questionId,
      String questionContent,
      String questionType,
      long optionId,
      String optionContent,
      int optionSortNo) {}
}

