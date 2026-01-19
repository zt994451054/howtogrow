package com.howtogrow.backend.infrastructure.parenting;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

@Repository
public class DailyParentingStatusRepository {
  private final NamedParameterJdbcTemplate jdbc;

  public DailyParentingStatusRepository(NamedParameterJdbcTemplate jdbc) {
    this.jdbc = jdbc;
  }

  public void upsert(long userId, long childId, LocalDate recordDate, String statusCode) {
    var existing = findId(userId, childId, recordDate);
    if (existing.isPresent()) {
      jdbc.update(
          """
          UPDATE daily_parenting_status
          SET status_code = :statusCode, updated_at = NOW(3)
          WHERE id = :id
          """,
          Map.of("id", existing.get(), "statusCode", statusCode));
      return;
    }

    KeyHolder kh = new GeneratedKeyHolder();
    jdbc.update(
        """
        INSERT INTO daily_parenting_status(user_id, child_id, record_date, status_code, created_at, updated_at)
        VALUES (:userId, :childId, :recordDate, :statusCode, NOW(3), NOW(3))
        """,
        new org.springframework.jdbc.core.namedparam.MapSqlParameterSource()
            .addValue("userId", userId)
            .addValue("childId", childId)
            .addValue("recordDate", recordDate)
            .addValue("statusCode", statusCode),
        kh);
    if (kh.getKey() == null) {
      throw new IllegalStateException("failed to insert daily_parenting_status");
    }
  }

  public Optional<String> findStatusCode(long userId, long childId, LocalDate recordDate) {
    var sql =
        """
        SELECT status_code
        FROM daily_parenting_status
        WHERE user_id = :userId AND child_id = :childId AND record_date = :recordDate
        LIMIT 1
        """;
    var rows =
        jdbc.queryForList(
            sql, Map.of("userId", userId, "childId", childId, "recordDate", recordDate), String.class);
    return rows.stream().findFirst();
  }

  public Map<LocalDate, String> findStatusCodeByDayBetween(
      long userId, long childId, LocalDate fromInclusive, LocalDate toInclusive) {
    var sql =
        """
        SELECT record_date, status_code
        FROM daily_parenting_status
        WHERE user_id = :userId
          AND child_id = :childId
          AND record_date >= :fromInclusive
          AND record_date <= :toInclusive
        ORDER BY record_date ASC
        """;
    var rows =
        jdbc.query(
            sql,
            Map.of("userId", userId, "childId", childId, "fromInclusive", fromInclusive, "toInclusive", toInclusive),
            (rs, rowNum) -> Map.entry(rs.getDate("record_date").toLocalDate(), rs.getString("status_code")));
    var map = new HashMap<LocalDate, String>();
    for (var e : rows) {
      map.put(e.getKey(), e.getValue());
    }
    return map;
  }

  private Optional<Long> findId(long userId, long childId, LocalDate recordDate) {
    var sql =
        """
        SELECT id
        FROM daily_parenting_status
        WHERE user_id = :userId AND child_id = :childId AND record_date = :recordDate
        LIMIT 1
        """;
    var rows =
        jdbc.queryForList(
            sql, Map.of("userId", userId, "childId", childId, "recordDate", recordDate), Long.class);
    return rows.stream().findFirst();
  }
}
