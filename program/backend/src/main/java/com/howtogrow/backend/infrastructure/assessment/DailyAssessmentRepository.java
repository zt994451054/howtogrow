package com.howtogrow.backend.infrastructure.assessment;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

@Repository
public class DailyAssessmentRepository {
  private static final RowMapper<DailyAssessment> ROW_MAPPER = (rs, rowNum) -> toDailyAssessment(rs);
  private final NamedParameterJdbcTemplate jdbc;

  public DailyAssessmentRepository(NamedParameterJdbcTemplate jdbc) {
    this.jdbc = jdbc;
  }

  public Optional<DailyAssessment> findById(long assessmentId) {
    var sql =
        """
        SELECT id, user_id, child_id, submitted_at
        FROM daily_assessment
        WHERE id = :id
        """;
    var rows = jdbc.query(sql, Map.of("id", assessmentId), ROW_MAPPER);
    return rows.stream().findFirst();
  }

  public boolean existsSubmittedBetween(long userId, long childId, Instant fromInclusive, Instant toExclusive) {
    var sql =
        """
        SELECT COUNT(*)
        FROM daily_assessment
        WHERE user_id = :userId
          AND child_id = :childId
          AND submitted_at >= :fromInclusive
          AND submitted_at < :toExclusive
        """;
    Integer count =
        jdbc.queryForObject(
            sql,
            Map.of(
                "userId", userId,
                "childId", childId,
                "fromInclusive", Timestamp.from(fromInclusive),
                "toExclusive", Timestamp.from(toExclusive)),
            Integer.class);
    return count != null && count > 0;
  }

  public long insertSubmitted(long userId, long childId, Instant submittedAt) {
    KeyHolder kh = new GeneratedKeyHolder();
    var sql =
        """
        INSERT INTO daily_assessment(user_id, child_id, submitted_at, created_at, updated_at)
        VALUES (:userId, :childId, :submittedAt, NOW(3), NOW(3))
        """;
    jdbc.update(
        sql,
        new org.springframework.jdbc.core.namedparam.MapSqlParameterSource()
            .addValue("userId", userId)
            .addValue("childId", childId)
            .addValue("submittedAt", Timestamp.from(submittedAt)),
        kh);
    var id = kh.getKey();
    if (id == null) {
      throw new IllegalStateException("failed to create daily_assessment");
    }
    return id.longValue();
  }

  private static DailyAssessment toDailyAssessment(ResultSet rs) throws SQLException {
    Instant submittedAt = null;
    var ts = rs.getTimestamp("submitted_at");
    if (ts != null) {
      submittedAt = ts.toInstant();
    }
    return new DailyAssessment(
        rs.getLong("id"),
        rs.getLong("user_id"),
        rs.getLong("child_id"),
        submittedAt);
  }
}
