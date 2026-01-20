package com.howtogrow.backend.infrastructure.admin;

import java.time.Instant;
import java.util.List;
import com.howtogrow.backend.infrastructure.db.SqlPagination;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class UserQueryRepository {
  private final NamedParameterJdbcTemplate jdbc;

  public UserQueryRepository(NamedParameterJdbcTemplate jdbc) {
    this.jdbc = jdbc;
  }

  public long countUsers(
      Long userId, String keyword, Boolean freeTrialUsed, String subscriptionStatus) {
    var params = new MapSqlParameterSource();
    var where = buildWhereSql(params, userId, keyword, freeTrialUsed, subscriptionStatus);
    var sql = "SELECT COUNT(*) FROM user_account" + where;
    Long count = jdbc.queryForObject(sql, params, Long.class);
    return count == null ? 0L : count;
  }

  public List<UserRow> listUsers(
      int offset, int limit, Long userId, String keyword, Boolean freeTrialUsed, String subscriptionStatus) {
    var params = new MapSqlParameterSource();
    var where = buildWhereSql(params, userId, keyword, freeTrialUsed, subscriptionStatus);
    var sql =
        """
        SELECT id, wechat_openid, nickname, avatar_url, subscription_end_at, free_trial_used, created_at
        FROM user_account
        """
            + where
            + """
        ORDER BY id DESC
        """;
    sql = sql + SqlPagination.limitOffset(offset, limit);
    return jdbc.query(
        sql,
        params,
        (rs, rowNum) -> {
          Instant subscriptionEndAt = null;
          var ts = rs.getTimestamp("subscription_end_at");
          if (ts != null) {
            subscriptionEndAt = ts.toInstant();
          }
          Instant createdAt = null;
          var cts = rs.getTimestamp("created_at");
          if (cts != null) {
            createdAt = cts.toInstant();
          }
          return new UserRow(
              rs.getLong("id"),
              rs.getString("wechat_openid"),
              rs.getString("nickname"),
              rs.getString("avatar_url"),
              subscriptionEndAt,
              rs.getBoolean("free_trial_used"),
              createdAt);
        });
  }

  private static String buildWhereSql(
      MapSqlParameterSource params,
      Long userId,
      String keyword,
      Boolean freeTrialUsed,
      String subscriptionStatus) {
    var sql = new StringBuilder(" WHERE deleted_at IS NULL");

    if (userId != null) {
      sql.append(" AND id = :userId");
      params.addValue("userId", userId);
    }

    if (keyword != null && !keyword.trim().isBlank()) {
      sql.append(" AND (wechat_openid LIKE :keyword OR nickname LIKE :keyword)");
      params.addValue("keyword", "%" + keyword.trim() + "%");
    }

    if (freeTrialUsed != null) {
      sql.append(" AND free_trial_used = :freeTrialUsed");
      params.addValue("freeTrialUsed", freeTrialUsed);
    }

    if (subscriptionStatus != null && !subscriptionStatus.trim().isBlank()) {
      var code = subscriptionStatus.trim().toUpperCase();
      switch (code) {
        case "ACTIVE" -> sql.append(" AND subscription_end_at IS NOT NULL AND subscription_end_at > NOW(3)");
        case "EXPIRED" -> sql.append(" AND subscription_end_at IS NOT NULL AND subscription_end_at <= NOW(3)");
        case "NONE" -> sql.append(" AND subscription_end_at IS NULL");
        default -> {
          // Ignore unknown values to keep the list endpoint resilient to UI changes.
        }
      }
    }

    // Ensure a token boundary between the WHERE clause and the following SQL segment.
    sql.append(" ");
    return sql.toString();
  }

  public record UserRow(
      long id,
      String wechatOpenid,
      String nickname,
      String avatarUrl,
      Instant subscriptionEndAt,
      boolean freeTrialUsed,
      Instant createdAt) {}
}
