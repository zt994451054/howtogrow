package com.howtogrow.backend.infrastructure.admin;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import com.howtogrow.backend.infrastructure.db.SqlPagination;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class UserQueryRepository {
  private final NamedParameterJdbcTemplate jdbc;

  public UserQueryRepository(NamedParameterJdbcTemplate jdbc) {
    this.jdbc = jdbc;
  }

  public long countUsers() {
    var sql = "SELECT COUNT(*) FROM user_account WHERE deleted_at IS NULL";
    Long count = jdbc.queryForObject(sql, Map.of(), Long.class);
    return count == null ? 0L : count;
  }

  public List<UserRow> listUsers(int offset, int limit) {
    var sql =
        """
        SELECT id, wechat_openid, nickname, avatar_url, subscription_end_at, free_trial_used, created_at
        FROM user_account
        WHERE deleted_at IS NULL
        ORDER BY id DESC
        """;
    sql = sql + SqlPagination.limitOffset(offset, limit);
    return jdbc.query(
        sql,
        Map.of(),
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

  public record UserRow(
      long id,
      String wechatOpenid,
      String nickname,
      String avatarUrl,
      Instant subscriptionEndAt,
      boolean freeTrialUsed,
      Instant createdAt) {}
}
