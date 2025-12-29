package com.howtogrow.backend.infrastructure.subscription;

import java.time.Instant;
import java.util.Map;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class SubscriptionGrantRepository {
  private final NamedParameterJdbcTemplate jdbc;

  public SubscriptionGrantRepository(NamedParameterJdbcTemplate jdbc) {
    this.jdbc = jdbc;
  }

  public boolean insertIfAbsent(
      long userId,
      long orderId,
      long planId,
      int daysGranted,
      Instant grantedFrom,
      Instant grantedTo) {
    var sql =
        """
        INSERT IGNORE INTO subscription_grant(
          user_id, order_id, plan_id, days_granted, granted_from, granted_to, created_at
        )
        VALUES (:userId, :orderId, :planId, :daysGranted, :grantedFrom, :grantedTo, NOW(3))
        """;
    return jdbc.update(
            sql,
            Map.of(
                "userId", userId,
                "orderId", orderId,
                "planId", planId,
                "daysGranted", daysGranted,
                "grantedFrom", java.sql.Timestamp.from(grantedFrom),
                "grantedTo", java.sql.Timestamp.from(grantedTo)))
        == 1;
  }
}

