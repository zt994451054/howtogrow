package com.howtogrow.backend.service.miniprogram;

import com.howtogrow.backend.api.ErrorCode;
import com.howtogrow.backend.api.ApiResponse;
import com.howtogrow.backend.api.TraceId;
import com.howtogrow.backend.api.exception.AppException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.howtogrow.backend.controller.miniprogram.dto.AiChatCreateSessionResponse;
import com.howtogrow.backend.controller.miniprogram.dto.AiChatMessageCreateResponse;
import com.howtogrow.backend.controller.miniprogram.dto.AiChatMessageView;
import com.howtogrow.backend.controller.miniprogram.dto.AiChatSessionView;
import com.howtogrow.backend.infrastructure.aichat.AiChatMessageRepository;
import com.howtogrow.backend.infrastructure.aichat.AiChatSessionRepository;
import com.howtogrow.backend.infrastructure.ai.AiChatClient;
import com.howtogrow.backend.infrastructure.ai.OpenAiStreamClient;
import com.howtogrow.backend.config.RateLimitProperties;
import com.howtogrow.backend.service.common.FixedWindowRateLimiter;
import com.howtogrow.backend.service.common.SubscriptionService;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import org.springframework.core.task.TaskExecutor;
import org.springframework.http.MediaType;

@Service
public class AiChatService {
  private static final int CONTEXT_LIMIT = 20;
  private static final String STREAM_SYSTEM_PROMPT =
      """
      你是一位资深育儿专家与亲子沟通教练（偏正向育儿）。
      请先共情，再给出具体、可执行、分步骤的建议；必要时提供可直接照读的话术示例。
      不做医疗/心理诊断，不贴标签；涉及安全风险（自伤/他伤/虐待/危险行为）时，优先提示寻求线下专业帮助。
      回答尽量简洁清晰，结构化输出（要点/步骤/示例）。
      输出格式要求：始终使用 Markdown 格式回复（可用标题/列表/加粗/引用等）。
      """
          .trim();

  private final SubscriptionService subscriptionService;
  private final AiChatSessionRepository sessionRepo;
  private final AiChatMessageRepository messageRepo;
  private final OpenAiStreamClient openAiStreamClient;
  private final ObjectMapper objectMapper;
  private final TaskExecutor taskExecutor;
  private final FixedWindowRateLimiter rateLimiter;
  private final RateLimitProperties rateLimitProperties;

  public AiChatService(
      SubscriptionService subscriptionService,
      AiChatSessionRepository sessionRepo,
      AiChatMessageRepository messageRepo,
      OpenAiStreamClient openAiStreamClient,
      ObjectMapper objectMapper,
      TaskExecutor taskExecutor,
      FixedWindowRateLimiter rateLimiter,
      RateLimitProperties rateLimitProperties) {
    this.subscriptionService = subscriptionService;
    this.sessionRepo = sessionRepo;
    this.messageRepo = messageRepo;
    this.openAiStreamClient = openAiStreamClient;
    this.objectMapper = objectMapper;
    this.taskExecutor = taskExecutor;
    this.rateLimiter = rateLimiter;
    this.rateLimitProperties = rateLimitProperties;
  }

  @Transactional
  public AiChatCreateSessionResponse createSession(long userId, Long childId) {
    subscriptionService.requireSubscribed(userId);
    if (childId != null && childId <= 0) {
      throw new AppException(ErrorCode.INVALID_REQUEST, "childId 必须为正数");
    }
    var sessionId = sessionRepo.create(userId, childId);
    return new AiChatCreateSessionResponse(sessionId);
  }

  public List<AiChatSessionView> listSessions(long userId, int limit) {
    subscriptionService.requireSubscribed(userId);
    var safeLimit = Math.max(1, Math.min(100, limit));
    return sessionRepo.listByUser(userId, safeLimit).stream()
        .map(s -> new AiChatSessionView(s.id(), s.childId(), s.title(), s.status(), s.lastActiveAt()))
        .toList();
  }

  public List<AiChatMessageView> listMessages(long userId, long sessionId, int limit, Long beforeMessageId) {
    subscriptionService.requireSubscribed(userId);
    var safeLimit = Math.max(1, Math.min(100, limit));
    Long safeBefore = null;
    if (beforeMessageId != null && beforeMessageId > 0) {
      safeBefore = beforeMessageId;
    }
    var session =
        sessionRepo.findById(sessionId).orElseThrow(() -> new AppException(ErrorCode.NOT_FOUND, "会话不存在"));
    if (session.userId() != userId) {
      throw new AppException(ErrorCode.FORBIDDEN_RESOURCE, "无权限");
    }
    return messageRepo.listPageDesc(sessionId, safeLimit, safeBefore).stream()
        .map(m -> new AiChatMessageView(m.id(), m.role(), m.content(), m.createdAt()))
        .toList();
  }

  @Transactional
  public AiChatMessageCreateResponse createUserMessage(long userId, long sessionId, String content) {
    subscriptionService.requireSubscribed(userId);
    rateLimiter.require(
        "ai-chat:user:" + userId,
        Duration.ofMinutes(1),
        Math.max(0, rateLimitProperties.aiChatPerMinute()));
    var session =
        sessionRepo.findById(sessionId).orElseThrow(() -> new AppException(ErrorCode.NOT_FOUND, "会话不存在"));
    if (session.userId() != userId) {
      throw new AppException(ErrorCode.FORBIDDEN_RESOURCE, "无权限");
    }
    if (!"ACTIVE".equalsIgnoreCase(session.status())) {
      throw new AppException(ErrorCode.INVALID_REQUEST, "会话不可用");
    }

    var trimmed = content.trim();
    sessionRepo.setTitleIfBlank(sessionId, AiChatTitleNormalizer.normalizeForTitle(trimmed));
    var msgId = messageRepo.insert(sessionId, userId, "user", trimmed);
    sessionRepo.touch(sessionId);
    return new AiChatMessageCreateResponse(msgId);
  }

  public SseEmitter streamAssistantReply(long userId, long sessionId) {
    subscriptionService.requireSubscribed(userId);
    var session =
        sessionRepo.findById(sessionId).orElseThrow(() -> new AppException(ErrorCode.NOT_FOUND, "会话不存在"));
    if (session.userId() != userId) {
      throw new AppException(ErrorCode.FORBIDDEN_RESOURCE, "无权限");
    }

    var emitter = new SseEmitter(60_000L);
    var context = messageRepo.listRecent(sessionId, CONTEXT_LIMIT);
    if (context.isEmpty()) {
      emitter.complete();
      return emitter;
    }

    taskExecutor.execute(
        () -> {
          try {
            var replyBuilder = new StringBuilder();
            var messages = new ArrayList<AiChatClient.ChatMessage>();
            messages.add(new AiChatClient.ChatMessage("system", STREAM_SYSTEM_PROMPT));
            messages.addAll(toChatMessages(context));
            openAiStreamClient.streamChatCompletions(
                messages,
                delta -> {
                  if (delta == null || delta.isEmpty()) {
                    return;
                  }
                  try {
                    replyBuilder.append(delta);
                    emitter.send(SseEmitter.event().name("delta").data(delta, MediaType.TEXT_PLAIN));
                  } catch (Exception e) {
                    throw new RuntimeException(e);
                  }
                },
                done -> {
                  try {
                    emitter.send(SseEmitter.event().name("done").data("[DONE]", MediaType.TEXT_PLAIN));
                  } catch (Exception e) {
                    throw new RuntimeException(e);
                  }
                });

            var reply = replyBuilder.toString().trim();
            if (!reply.isBlank()) {
              messageRepo.insert(sessionId, userId, "assistant", reply);
              sessionRepo.touch(sessionId);
            }
            emitter.complete();
          } catch (Exception e) {
            sendSseError(emitter, e);
            emitter.complete();
          }
        });
    return emitter;
  }

  private void sendSseError(SseEmitter emitter, Exception e) {
    var code = ErrorCode.INTERNAL_ERROR.name();
    var message = "服务异常";
    if (e instanceof AppException ae) {
      code = ae.code().name();
      message = ae.getMessage();
    }
    var traceId = TraceId.current();
    var payload = ApiResponse.error(code, message, traceId);
    try {
      emitter.send(SseEmitter.event().name("error").data(objectMapper.writeValueAsString(payload), MediaType.TEXT_PLAIN));
    } catch (Exception ignored) {
      // ignore
    }
  }

  private static List<AiChatClient.ChatMessage> toChatMessages(
      List<AiChatMessageRepository.AiChatMessageRow> rows) {
    return rows.stream()
        .map(r -> new AiChatClient.ChatMessage(r.role(), r.content()))
        .toList();
  }

}
