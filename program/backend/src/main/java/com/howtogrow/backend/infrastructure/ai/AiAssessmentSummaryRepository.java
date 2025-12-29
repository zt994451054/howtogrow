package com.howtogrow.backend.infrastructure.ai;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Map;
import java.util.Optional;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

@Repository
public class AiAssessmentSummaryRepository {
  private static final RowMapper<AiAssessmentSummaryRow> ROW_MAPPER =
      (rs, rowNum) -> toRow(rs);

  private final NamedParameterJdbcTemplate jdbc;

  public AiAssessmentSummaryRepository(NamedParameterJdbcTemplate jdbc) {
    this.jdbc = jdbc;
  }

  public Optional<AiAssessmentSummaryRow> findByAssessmentId(long assessmentId) {
    var sql =
        """
        SELECT id, assessment_id, user_id, content
        FROM ai_assessment_summary
        WHERE assessment_id = :assessmentId
        """;
    var rows = jdbc.query(sql, Map.of("assessmentId", assessmentId), ROW_MAPPER);
    return rows.stream().findFirst();
  }

  public long insert(long assessmentId, long userId, String content) {
    KeyHolder kh = new GeneratedKeyHolder();
    var sql =
        """
        INSERT INTO ai_assessment_summary(assessment_id, user_id, content, created_at)
        VALUES (:assessmentId, :userId, :content, NOW(3))
        """;
    jdbc.update(
        sql,
        new org.springframework.jdbc.core.namedparam.MapSqlParameterSource()
            .addValue("assessmentId", assessmentId)
            .addValue("userId", userId)
            .addValue("content", content),
        kh);
    var id = kh.getKey();
    if (id == null) {
      throw new IllegalStateException("failed to insert ai_assessment_summary");
    }
    return id.longValue();
  }

  private static AiAssessmentSummaryRow toRow(ResultSet rs) throws SQLException {
    return new AiAssessmentSummaryRow(
        rs.getLong("id"),
        rs.getLong("assessment_id"),
        rs.getLong("user_id"),
        rs.getString("content"));
  }

  public record AiAssessmentSummaryRow(long id, long assessmentId, long userId, String content) {}
}
