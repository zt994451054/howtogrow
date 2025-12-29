package com.howtogrow.backend.infrastructure.report;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class GrowthReportRepository {
  private final NamedParameterJdbcTemplate jdbc;

  public GrowthReportRepository(NamedParameterJdbcTemplate jdbc) {
    this.jdbc = jdbc;
  }

  public List<GrowthRow> listDailyDimensionScores(
      long userId, long childId, LocalDate from, LocalDate to) {
    var sql =
        """
        SELECT
          DATE(a.submitted_at) AS biz_date,
          ds.dimension_code,
          SUM(ds.score) AS total_score
        FROM daily_assessment a
        JOIN daily_assessment_dimension_score ds ON ds.assessment_id = a.id
        WHERE a.user_id = :userId
          AND a.child_id = :childId
          AND DATE(a.submitted_at) BETWEEN :from AND :to
        GROUP BY DATE(a.submitted_at), ds.dimension_code
        ORDER BY biz_date ASC, ds.dimension_code ASC
        """;
    return jdbc.query(
        sql,
        Map.of("userId", userId, "childId", childId, "from", from, "to", to),
        (rs, rowNum) ->
            new GrowthRow(
                rs.getObject("biz_date", LocalDate.class),
                rs.getString("dimension_code"),
                rs.getLong("total_score")));
  }

  public record GrowthRow(
      LocalDate bizDate, String dimensionCode, long score) {}
}
