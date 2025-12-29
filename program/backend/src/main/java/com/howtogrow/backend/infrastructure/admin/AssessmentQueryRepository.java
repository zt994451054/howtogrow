package com.howtogrow.backend.infrastructure.admin;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import com.howtogrow.backend.infrastructure.db.SqlPagination;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class AssessmentQueryRepository {
  private final NamedParameterJdbcTemplate jdbc;

  public AssessmentQueryRepository(NamedParameterJdbcTemplate jdbc) {
    this.jdbc = jdbc;
  }

  public long countAssessments() {
    var sql = "SELECT COUNT(*) FROM daily_assessment";
    Long count = jdbc.queryForObject(sql, Map.of(), Long.class);
    return count == null ? 0L : count;
  }

  public List<AssessmentRow> listAssessments(int offset, int limit) {
    var sql =
        """
        SELECT
          a.id,
          a.user_id,
          u.nickname AS user_nickname,
          a.child_id,
          c.nickname AS child_nickname,
          DATE(a.submitted_at) AS biz_date,
          a.submitted_at
        FROM daily_assessment a
        JOIN user_account u ON u.id = a.user_id
        JOIN child c ON c.id = a.child_id
        ORDER BY a.id DESC
        """
            + SqlPagination.limitOffset(offset, limit);
    return jdbc.query(
        sql,
        Map.of(),
        (rs, rowNum) -> {
          Instant submittedAt = null;
          var subts = rs.getTimestamp("submitted_at");
          if (subts != null) {
            submittedAt = subts.toInstant();
          }
          return new AssessmentRow(
              rs.getLong("id"),
              rs.getLong("user_id"),
              rs.getString("user_nickname"),
              rs.getLong("child_id"),
              rs.getString("child_nickname"),
              rs.getObject("biz_date", LocalDate.class),
              submittedAt);
        });
  }

  public record AssessmentRow(
      long id,
      long userId,
      String userNickname,
      long childId,
      String childNickname,
      LocalDate bizDate,
      Instant submittedAt) {}
}
