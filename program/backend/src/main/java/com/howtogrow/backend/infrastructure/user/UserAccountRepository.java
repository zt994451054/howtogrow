package com.howtogrow.backend.infrastructure.user;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

@Repository
public class UserAccountRepository {
  private static final RowMapper<UserAccount> ROW_MAPPER =
      (rs, rowNum) -> toUserAccount(rs);

  private final NamedParameterJdbcTemplate jdbc;

  public UserAccountRepository(NamedParameterJdbcTemplate jdbc) {
    this.jdbc = jdbc;
  }

  public Optional<UserAccount> findById(long id) {
    var sql =
        """
        SELECT id, wechat_openid, nickname, avatar_url, subscription_end_at, free_trial_used
        FROM user_account
        WHERE id = :id AND deleted_at IS NULL
        """;
    var rows = jdbc.query(sql, Map.of("id", id), ROW_MAPPER);
    return rows.stream().findFirst();
  }

  public Optional<UserAccount> findByWechatOpenid(String openid) {
    var sql =
        """
        SELECT id, wechat_openid, nickname, avatar_url, subscription_end_at, free_trial_used
        FROM user_account
        WHERE wechat_openid = :openid AND deleted_at IS NULL
        """;
    var rows = jdbc.query(sql, Map.of("openid", openid), ROW_MAPPER);
    return rows.stream().findFirst();
  }

  public UserAccount create(String openid, String unionid) {
    KeyHolder kh = new GeneratedKeyHolder();
    var sql =
        """
        INSERT INTO user_account(wechat_openid, wechat_unionid, created_at, updated_at)
        VALUES (:openid, :unionid, NOW(3), NOW(3))
        """;
    jdbc.update(sql, new org.springframework.jdbc.core.namedparam.MapSqlParameterSource()
        .addValue("openid", openid)
        .addValue("unionid", unionid), kh);

    var id = kh.getKey();
    if (id == null) {
      throw new IllegalStateException("failed to create user_account");
    }
    return findById(id.longValue()).orElseThrow();
  }

  public void markFreeTrialUsed(long userId) {
    var sql =
        """
        UPDATE user_account
        SET free_trial_used = 1, updated_at = NOW(3)
        WHERE id = :userId AND deleted_at IS NULL AND free_trial_used = 0
        """;
    jdbc.update(sql, Map.of("userId", userId));
  }

  private static UserAccount toUserAccount(ResultSet rs) throws SQLException {
    Instant subscriptionEndAt = null;
    var ts = rs.getTimestamp("subscription_end_at");
    if (ts != null) {
      subscriptionEndAt = ts.toInstant();
    }
    return new UserAccount(
        rs.getLong("id"),
        rs.getString("wechat_openid"),
        rs.getString("nickname"),
        rs.getString("avatar_url"),
        subscriptionEndAt,
        rs.getBoolean("free_trial_used"));
  }
}
