package com.howtogrow.backend.infrastructure.admin;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import com.howtogrow.backend.infrastructure.db.SqlPagination;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class OrderQueryRepository {
  private final NamedParameterJdbcTemplate jdbc;

  public OrderQueryRepository(NamedParameterJdbcTemplate jdbc) {
    this.jdbc = jdbc;
  }

  public long countOrders() {
    var sql = "SELECT COUNT(*) FROM purchase_order";
    Long count = jdbc.queryForObject(sql, Map.of(), Long.class);
    return count == null ? 0L : count;
  }

  public List<OrderRow> listOrders(int offset, int limit) {
    var sql =
        """
        SELECT
          o.id,
          o.order_no,
          o.user_id,
          u.nickname AS user_nickname,
          u.avatar_url AS user_avatar_url,
          o.plan_id,
          p.name AS plan_name,
          o.amount_cent,
          o.status,
          o.pay_trade_no,
          o.prepay_id,
          o.created_at,
          o.paid_at
        FROM purchase_order o
        JOIN user_account u ON u.id = o.user_id
        JOIN subscription_plan p ON p.id = o.plan_id
        ORDER BY o.id DESC
        """;
    sql = sql + SqlPagination.limitOffset(offset, limit);
    return jdbc.query(
        sql,
        Map.of(),
        (rs, rowNum) -> {
          Instant createdAt = null;
          var cts = rs.getTimestamp("created_at");
          if (cts != null) {
            createdAt = cts.toInstant();
          }
          Instant paidAt = null;
          var pts = rs.getTimestamp("paid_at");
          if (pts != null) {
            paidAt = pts.toInstant();
          }
          return new OrderRow(
              rs.getLong("id"),
              rs.getString("order_no"),
              rs.getLong("user_id"),
              rs.getString("user_nickname"),
              rs.getString("user_avatar_url"),
              rs.getLong("plan_id"),
              rs.getString("plan_name"),
              rs.getInt("amount_cent"),
              rs.getString("status"),
              rs.getString("pay_trade_no"),
              rs.getString("prepay_id"),
              createdAt,
              paidAt);
        });
  }

  public record OrderRow(
      long id,
      String orderNo,
      long userId,
      String userNickname,
      String userAvatarUrl,
      long planId,
      String planName,
      int amountCent,
      String status,
      String payTradeNo,
      String prepayId,
      Instant createdAt,
      Instant paidAt) {}
}
