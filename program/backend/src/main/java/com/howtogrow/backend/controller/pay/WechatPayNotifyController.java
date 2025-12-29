package com.howtogrow.backend.controller.pay;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.howtogrow.backend.api.ErrorCode;
import com.howtogrow.backend.api.exception.AppException;
import com.howtogrow.backend.infrastructure.pay.WechatPayNotifyEventRepository;
import com.howtogrow.backend.service.pay.WechatPayNotifyService;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/pay/wechat")
public class WechatPayNotifyController {
  private static final Logger log = LoggerFactory.getLogger(WechatPayNotifyController.class);

  private final ObjectMapper objectMapper;
  private final WechatPayNotifyEventRepository eventRepo;
  private final WechatPayNotifyService notifyService;

  public WechatPayNotifyController(
      ObjectMapper objectMapper,
      WechatPayNotifyEventRepository eventRepo,
      WechatPayNotifyService notifyService) {
    this.objectMapper = objectMapper;
    this.eventRepo = eventRepo;
    this.notifyService = notifyService;
  }

  @PostMapping(value = "/notify", consumes = MediaType.APPLICATION_JSON_VALUE)
  public NotifyAck notify(@RequestBody String rawBody, HttpServletRequest request) throws IOException {
    var node = safeParse(rawBody);
    var eventId = textOrDefault(node, "id", "evt_" + UUID.randomUUID());
    var eventType = textOrDefault(node, "event_type", "");
    var resourceType = textOrDefault(node, "resource_type", "");
    var summary = textOrDefault(node, "summary", "");

    eventRepo.insertIfAbsent(eventId, eventType, resourceType, summary, rawBody);
    try {
      notifyService.process(eventId, node);
      eventRepo.markProcessed(eventId);
    } catch (AppException e) {
      eventRepo.markFailed(eventId, e.getMessage());
      throw e;
    } catch (Exception e) {
      eventRepo.markFailed(eventId, "internal error");
      log.error(
          "wechat pay notify failed, eventId={}, eventType={}, outTradeNo={}, transactionId={}",
          eventId,
          eventType,
          textOrDefault(node, "out_trade_no", ""),
          textOrDefault(node, "transaction_id", ""),
          e);
      throw new AppException(ErrorCode.INTERNAL_ERROR, "internal error");
    }
    return new NotifyAck("SUCCESS", "OK");
  }

  private JsonNode safeParse(String rawBody) throws IOException {
    if (rawBody == null || rawBody.isBlank()) {
      return objectMapper.createObjectNode();
    }
    return objectMapper.readTree(rawBody);
  }

  private static String textOrDefault(JsonNode node, String field, String fallback) {
    if (node == null) {
      return fallback;
    }
    var value = node.get(field);
    if (value == null || value.isNull()) {
      return fallback;
    }
    var text = value.asText();
    return text == null ? fallback : text;
  }

  public record NotifyAck(
      @Schema(description = "返回码") String code,
      @Schema(description = "返回信息") String message) {}
}
