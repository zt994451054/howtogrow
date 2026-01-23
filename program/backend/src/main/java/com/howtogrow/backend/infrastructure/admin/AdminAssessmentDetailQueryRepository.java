package com.howtogrow.backend.infrastructure.admin;

import java.sql.Timestamp;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Map;
import java.util.Optional;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class AdminAssessmentDetailQueryRepository {
  private final NamedParameterJdbcTemplate jdbc;

  public AdminAssessmentDetailQueryRepository(NamedParameterJdbcTemplate jdbc) {
    this.jdbc = jdbc;
  }

  public Optional<AssessmentDetailRow> findByAssessmentId(long assessmentId) {
    var sql =
        """
        SELECT
          a.id AS assessment_id,
          a.user_id,
          u.nickname AS user_nickname,
          u.avatar_url AS user_avatar_url,
          a.child_id,
          c.nickname AS child_nickname,
          DATE(a.submitted_at) AS biz_date,
          a.submitted_at,
          s.content AS ai_summary
        FROM daily_assessment a
        JOIN user_account u ON u.id = a.user_id
        JOIN child c ON c.id = a.child_id
        LEFT JOIN ai_assessment_summary s
          ON s.assessment_id = a.id
         AND s.user_id = a.user_id
        WHERE a.id = :assessmentId
        """;
    var rows =
        jdbc.query(
            sql,
            Map.of("assessmentId", assessmentId),
            (rs, rowNum) -> {
              Instant submittedAt = null;
              Timestamp ts = rs.getTimestamp("submitted_at");
              if (ts != null) submittedAt = ts.toInstant();
              return new AssessmentDetailRow(
                  rs.getLong("assessment_id"),
                  rs.getLong("user_id"),
                  rs.getString("user_nickname"),
                  rs.getString("user_avatar_url"),
                  rs.getLong("child_id"),
                  rs.getString("child_nickname"),
                  rs.getObject("biz_date", LocalDate.class),
                  submittedAt,
                  rs.getString("ai_summary"));
            });
    return rows.stream().findFirst();
  }

  public record AssessmentDetailRow(
      long assessmentId,
      long userId,
      String userNickname,
      String userAvatarUrl,
      long childId,
      String childNickname,
      LocalDate bizDate,
      Instant submittedAt,
      String aiSummary) {}
}

