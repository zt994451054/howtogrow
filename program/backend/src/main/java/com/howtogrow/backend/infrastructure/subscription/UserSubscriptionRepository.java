package com.howtogrow.backend.infrastructure.subscription;

import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class UserSubscriptionRepository {
  private final NamedParameterJdbcTemplate jdbc;

  public UserSubscriptionRepository(NamedParameterJdbcTemplate jdbc) {
    this.jdbc = jdbc;
  }

  public Optional<Instant> findSubscriptionEndAt(long userId) {
    var sql = "SELECT subscription_end_at FROM user_account WHERE id = :userId AND deleted_at IS NULL";
    return jdbc.query(
        sql,
        Map.of("userId", userId),
        rs -> {
          if (!rs.next()) {
            return Optional.<Instant>empty();
          }
          var ts = rs.getTimestamp(1);
          return ts == null ? Optional.<Instant>empty() : Optional.of(ts.toInstant());
        });
  }

  public void updateSubscriptionEndAt(long userId, Instant endAt) {
    var sql =
        """
        UPDATE user_account
        SET subscription_end_at = :endAt, updated_at = NOW(3)
        WHERE id = :userId AND deleted_at IS NULL
        """;
    jdbc.update(sql, Map.of("userId", userId, "endAt", java.sql.Timestamp.from(endAt)));
  }
}
