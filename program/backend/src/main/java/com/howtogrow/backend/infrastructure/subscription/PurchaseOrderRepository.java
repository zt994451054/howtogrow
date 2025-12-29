package com.howtogrow.backend.infrastructure.subscription;

import java.util.Map;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

@Repository
public class PurchaseOrderRepository {
  private final NamedParameterJdbcTemplate jdbc;

  public PurchaseOrderRepository(NamedParameterJdbcTemplate jdbc) {
    this.jdbc = jdbc;
  }

  public long createOrder(String orderNo, long userId, long planId, int amountCent, String prepayId) {
    KeyHolder kh = new GeneratedKeyHolder();
    var sql =
        """
        INSERT INTO purchase_order(
          order_no, user_id, plan_id, amount_cent, status, pay_channel, prepay_id, created_at
        )
        VALUES (:orderNo, :userId, :planId, :amountCent, 'CREATED', 'WECHAT', :prepayId, NOW(3))
        """;
    jdbc.update(
        sql,
        new org.springframework.jdbc.core.namedparam.MapSqlParameterSource()
            .addValue("orderNo", orderNo)
            .addValue("userId", userId)
            .addValue("planId", planId)
            .addValue("amountCent", amountCent)
            .addValue("prepayId", prepayId),
        kh);
    var id = kh.getKey();
    if (id == null) {
      throw new IllegalStateException("failed to create purchase_order");
    }
    return id.longValue();
  }

  public boolean existsOrderNo(String orderNo) {
    var sql = "SELECT COUNT(*) FROM purchase_order WHERE order_no = :orderNo";
    Integer count = jdbc.queryForObject(sql, Map.of("orderNo", orderNo), Integer.class);
    return count != null && count > 0;
  }

  public java.util.Optional<PurchaseOrderRow> findByOrderNo(String orderNo) {
    var sql =
        """
        SELECT id, order_no, user_id, plan_id, amount_cent, status, pay_trade_no, prepay_id, paid_at
        FROM purchase_order
        WHERE order_no = :orderNo
        """;
    var rows =
        jdbc.query(
            sql,
            Map.of("orderNo", orderNo),
            (rs, rowNum) -> {
              java.time.Instant paidAt = null;
              var ts = rs.getTimestamp("paid_at");
              if (ts != null) {
                paidAt = ts.toInstant();
              }
              return new PurchaseOrderRow(
                  rs.getLong("id"),
                  rs.getString("order_no"),
                  rs.getLong("user_id"),
                  rs.getLong("plan_id"),
                  rs.getInt("amount_cent"),
                  rs.getString("status"),
                  rs.getString("pay_trade_no"),
                  rs.getString("prepay_id"),
                  paidAt);
            });
    return rows.stream().findFirst();
  }

  public boolean markPaid(long orderId, String transactionId, java.time.Instant paidAt) {
    var sql =
        """
        UPDATE purchase_order
        SET status = 'PAID', pay_trade_no = :transactionId, paid_at = :paidAt
        WHERE id = :orderId AND status = 'CREATED'
        """;
    return jdbc.update(
            sql,
            Map.of(
                "orderId", orderId,
                "transactionId", transactionId,
                "paidAt", java.sql.Timestamp.from(paidAt)))
        == 1;
  }
}

