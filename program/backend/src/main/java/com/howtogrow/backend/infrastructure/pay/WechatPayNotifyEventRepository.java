package com.howtogrow.backend.infrastructure.pay;

import java.util.Map;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class WechatPayNotifyEventRepository {
  private final NamedParameterJdbcTemplate jdbc;

  public WechatPayNotifyEventRepository(NamedParameterJdbcTemplate jdbc) {
    this.jdbc = jdbc;
  }

  public void insertIfAbsent(
      String eventId, String eventType, String resourceType, String summary, String rawBody) {
    var sql =
        """
        INSERT IGNORE INTO wechat_pay_notify_event(
          event_id, event_type, resource_type, summary, raw_body, received_at, process_status
        )
        VALUES (:eventId, :eventType, :resourceType, :summary, :rawBody, NOW(3), 'RECEIVED')
        """;
    jdbc.update(
        sql,
        Map.of(
            "eventId", eventId,
            "eventType", eventType,
            "resourceType", resourceType,
            "summary", summary,
            "rawBody", rawBody));
  }

  public void markProcessed(String eventId) {
    var sql =
        """
        UPDATE wechat_pay_notify_event
        SET process_status = 'PROCESSED', processed_at = NOW(3)
        WHERE event_id = :eventId
        """;
    jdbc.update(sql, Map.of("eventId", eventId));
  }

  public void markFailed(String eventId, String reason) {
    var sql =
        """
        UPDATE wechat_pay_notify_event
        SET process_status = 'FAILED', processed_at = NOW(3), fail_reason = :reason
        WHERE event_id = :eventId
        """;
    jdbc.update(sql, Map.of("eventId", eventId, "reason", reason));
  }
}
