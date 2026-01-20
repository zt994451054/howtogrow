package com.howtogrow.backend.infrastructure.admin;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import com.howtogrow.backend.infrastructure.db.SqlPagination;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class AssessmentQueryRepository {
  private final NamedParameterJdbcTemplate jdbc;

  public AssessmentQueryRepository(NamedParameterJdbcTemplate jdbc) {
    this.jdbc = jdbc;
  }

  public long countAssessments(
      Long userId, Long childId, String keyword, LocalDate bizDateFrom, LocalDate bizDateTo) {
    var params = new MapSqlParameterSource();
    var where = buildWhereSql(params, userId, childId, keyword, bizDateFrom, bizDateTo);
    var sql =
        """
        SELECT COUNT(*)
        FROM daily_assessment a
        JOIN user_account u ON u.id = a.user_id
        JOIN child c ON c.id = a.child_id
        """
            + where;
    Long count = jdbc.queryForObject(sql, params, Long.class);
    return count == null ? 0L : count;
  }

  public List<AssessmentRow> listAssessments(
      int offset,
      int limit,
      Long userId,
      Long childId,
      String keyword,
      LocalDate bizDateFrom,
      LocalDate bizDateTo) {
    var params = new MapSqlParameterSource();
    var where = buildWhereSql(params, userId, childId, keyword, bizDateFrom, bizDateTo);
    var sql =
        """
        SELECT
          a.id,
          a.user_id,
          u.nickname AS user_nickname,
          u.avatar_url AS user_avatar_url,
          a.child_id,
          c.nickname AS child_nickname,
          DATE(a.submitted_at) AS biz_date,
          a.submitted_at
        FROM daily_assessment a
        JOIN user_account u ON u.id = a.user_id
        JOIN child c ON c.id = a.child_id
        """
            + where
            + "\nORDER BY a.id DESC\n"
            + SqlPagination.limitOffset(offset, limit);
    return jdbc.query(
        sql,
        params,
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
              rs.getString("user_avatar_url"),
              rs.getLong("child_id"),
              rs.getString("child_nickname"),
              rs.getObject("biz_date", LocalDate.class),
              submittedAt);
        });
  }

  private static String buildWhereSql(
      MapSqlParameterSource params,
      Long userId,
      Long childId,
      String keyword,
      LocalDate bizDateFrom,
      LocalDate bizDateTo) {
    var sql = new StringBuilder(" WHERE 1=1 ");

    if (userId != null) {
      sql.append(" AND a.user_id = :userId");
      params.addValue("userId", userId);
    }

    if (childId != null) {
      sql.append(" AND a.child_id = :childId");
      params.addValue("childId", childId);
    }

    if (keyword != null && !keyword.trim().isBlank()) {
      sql.append(" AND (u.nickname LIKE :keyword OR c.nickname LIKE :keyword)");
      params.addValue("keyword", "%" + keyword.trim() + "%");
    }

    if (bizDateFrom != null) {
      sql.append(" AND DATE(a.submitted_at) >= :bizDateFrom");
      params.addValue("bizDateFrom", bizDateFrom);
    }

    if (bizDateTo != null) {
      sql.append(" AND DATE(a.submitted_at) <= :bizDateTo");
      params.addValue("bizDateTo", bizDateTo);
    }

    return sql.toString();
  }

  public List<AssessmentDimensionScoreRow> listDimensionScoresByAssessmentIds(List<Long> assessmentIds) {
    if (assessmentIds == null || assessmentIds.isEmpty()) {
      return List.of();
    }
    var sql =
        """
        SELECT
          assessment_id,
          dimension_code,
          SUM(score) AS score_sum
        FROM daily_assessment_dimension_score
        WHERE assessment_id IN (:assessmentIds)
        GROUP BY assessment_id, dimension_code
        ORDER BY assessment_id ASC, dimension_code ASC
        """;
    return jdbc.query(
        sql,
        Map.of("assessmentIds", assessmentIds),
        (rs, rowNum) ->
            new AssessmentDimensionScoreRow(
                rs.getLong("assessment_id"),
                rs.getString("dimension_code"),
                rs.getLong("score_sum")));
  }

  public record AssessmentRow(
      long id,
      long userId,
      String userNickname,
      String userAvatarUrl,
      long childId,
      String childNickname,
      LocalDate bizDate,
      Instant submittedAt) {}

  public record AssessmentDimensionScoreRow(long assessmentId, String dimensionCode, long score) {}
}
