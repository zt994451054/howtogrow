package com.howtogrow.backend.infrastructure.assessment;

import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

@Repository
public class DailyAssessmentItemRepository {
  private final NamedParameterJdbcTemplate jdbc;

  public DailyAssessmentItemRepository(NamedParameterJdbcTemplate jdbc) {
    this.jdbc = jdbc;
  }

  public long insertItem(long assessmentId, long questionId, int displayOrder) {
    KeyHolder kh = new GeneratedKeyHolder();
    var sql =
        """
        INSERT INTO daily_assessment_item(assessment_id, question_id, display_order, created_at, updated_at)
        VALUES (:assessmentId, :questionId, :displayOrder, NOW(3), NOW(3))
        """;
    jdbc.update(
        sql,
        new org.springframework.jdbc.core.namedparam.MapSqlParameterSource()
            .addValue("assessmentId", assessmentId)
            .addValue("questionId", questionId)
            .addValue("displayOrder", displayOrder),
        kh);
    var id = kh.getKey();
    if (id == null) {
      throw new IllegalStateException("failed to create daily_assessment_item");
    }
    return id.longValue();
  }
}
