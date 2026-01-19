package com.howtogrow.backend.infrastructure.report;

import java.time.LocalDate;
import java.util.Map;
import java.util.Optional;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class AwarenessPersistenceRepository {
  private final NamedParameterJdbcTemplate jdbc;

  public AwarenessPersistenceRepository(NamedParameterJdbcTemplate jdbc) {
    this.jdbc = jdbc;
  }

  public Optional<LocalDate> findFirstRecordDate(long userId, long childId) {
    var sql =
        """
        SELECT MIN(d) AS first_date
        FROM (
          SELECT MIN(record_date) AS d
          FROM daily_parenting_status
          WHERE user_id = :userId AND child_id = :childId

          UNION ALL
          SELECT MIN(record_date) AS d
          FROM daily_trouble_record
          WHERE user_id = :userId AND child_id = :childId

          UNION ALL
          SELECT MIN(record_date) AS d
          FROM daily_parenting_diary
          WHERE user_id = :userId AND child_id = :childId

          UNION ALL
          SELECT MIN(DATE(submitted_at)) AS d
          FROM daily_assessment
          WHERE user_id = :userId AND child_id = :childId
        ) t
        """;
    var first =
        jdbc.query(
            sql,
            Map.of("userId", userId, "childId", childId),
            rs -> rs.next() ? rs.getObject("first_date", LocalDate.class) : null);
    return Optional.ofNullable(first);
  }
}

