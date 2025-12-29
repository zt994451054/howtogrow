package com.howtogrow.backend.infrastructure.pay;

import java.time.Instant;
import java.util.Map;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class WechatPayTransactionRepository {
  private final NamedParameterJdbcTemplate jdbc;

  public WechatPayTransactionRepository(NamedParameterJdbcTemplate jdbc) {
    this.jdbc = jdbc;
  }

  public void upsertTransaction(
      long orderId,
      String orderNo,
      String mchId,
      String appid,
      String transactionId,
      String tradeType,
      String tradeState,
      String tradeStateDesc,
      String bankType,
      String payerOpenid,
      int totalAmountCent,
      String currency,
      Instant successTime,
      String latestEventId) {
    var sql =
        """
        INSERT INTO wechat_pay_transaction(
          order_id, order_no, mch_id, appid, transaction_id, trade_type, trade_state, trade_state_desc,
          bank_type, payer_openid, total_amount_cent, currency, success_time, latest_event_id, created_at, updated_at
        )
        VALUES (
          :orderId, :orderNo, :mchId, :appid, :transactionId, :tradeType, :tradeState, :tradeStateDesc,
          :bankType, :payerOpenid, :totalAmountCent, :currency, :successTime, :latestEventId, NOW(3), NOW(3)
        )
        ON DUPLICATE KEY UPDATE
          trade_state = :tradeState,
          trade_state_desc = :tradeStateDesc,
          bank_type = :bankType,
          payer_openid = :payerOpenid,
          total_amount_cent = :totalAmountCent,
          currency = :currency,
          success_time = :successTime,
          latest_event_id = :latestEventId,
          updated_at = NOW(3)
        """;
    var params = new java.util.HashMap<String, Object>();
    params.put("orderId", orderId);
    params.put("orderNo", orderNo);
    params.put("mchId", mchId);
    params.put("appid", appid);
    params.put("transactionId", transactionId);
    params.put("tradeType", tradeType);
    params.put("tradeState", tradeState);
    params.put("tradeStateDesc", tradeStateDesc);
    params.put("bankType", bankType);
    params.put("payerOpenid", payerOpenid);
    params.put("totalAmountCent", totalAmountCent);
    params.put("currency", currency == null || currency.isBlank() ? "CNY" : currency);
    params.put("successTime", successTime == null ? null : java.sql.Timestamp.from(successTime));
    params.put("latestEventId", latestEventId);
    jdbc.update(sql, params);
  }
}
