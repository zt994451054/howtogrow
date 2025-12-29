package com.howtogrow.backend.infrastructure.question;

import java.util.List;
import java.util.Map;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class QuestionOptionRepository {
  private final NamedParameterJdbcTemplate jdbc;

  public QuestionOptionRepository(NamedParameterJdbcTemplate jdbc) {
    this.jdbc = jdbc;
  }

  public boolean optionsBelongToQuestion(long questionId, List<Long> optionIds) {
    if (optionIds == null || optionIds.isEmpty()) {
      return false;
    }
    var sql =
        """
        SELECT COUNT(*)
        FROM question_option
        WHERE question_id = :questionId AND deleted_at IS NULL
          AND id IN (:optionIds)
        """;
    Integer count =
        jdbc.queryForObject(sql, Map.of("questionId", questionId, "optionIds", optionIds), Integer.class);
    return count != null && count == optionIds.size();
  }
}

