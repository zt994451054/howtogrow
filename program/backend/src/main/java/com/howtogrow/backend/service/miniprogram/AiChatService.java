package com.howtogrow.backend.service.miniprogram;

import com.howtogrow.backend.api.ErrorCode;
import com.howtogrow.backend.api.exception.AppException;
import com.howtogrow.backend.controller.miniprogram.dto.AiChatCreateSessionResponse;
import com.howtogrow.backend.controller.miniprogram.dto.AiChatMessageCreateResponse;
import com.howtogrow.backend.controller.miniprogram.dto.AiChatSessionView;
import com.howtogrow.backend.infrastructure.aichat.AiChatMessageRepository;
import com.howtogrow.backend.infrastructure.aichat.AiChatSessionRepository;
import com.howtogrow.backend.infrastructure.ai.AiChatClient;
import com.howtogrow.backend.infrastructure.ai.AiChatClientProvider;
import com.howtogrow.backend.infrastructure.ai.OpenAiStreamClient;
import com.howtogrow.backend.infrastructure.ai.AiProperties;
import com.howtogrow.backend.config.RateLimitProperties;
import com.howtogrow.backend.service.common.FixedWindowRateLimiter;
import com.howtogrow.backend.service.common.SubscriptionService;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
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

  private final SubscriptionService subscriptionService;
  private final AiChatSessionRepository sessionRepo;
  private final AiChatMessageRepository messageRepo;
  private final AiChatClientProvider aiChatClientProvider;
  private final AiProperties aiProperties;
  private final OpenAiStreamClient openAiStreamClient;
  private final TaskExecutor taskExecutor;
  private final FixedWindowRateLimiter rateLimiter;
  private final RateLimitProperties rateLimitProperties;

  public AiChatService(
      SubscriptionService subscriptionService,
      AiChatSessionRepository sessionRepo,
      AiChatMessageRepository messageRepo,
      AiChatClientProvider aiChatClientProvider,
      AiProperties aiProperties,
      OpenAiStreamClient openAiStreamClient,
      TaskExecutor taskExecutor,
      FixedWindowRateLimiter rateLimiter,
      RateLimitProperties rateLimitProperties) {
    this.subscriptionService = subscriptionService;
    this.sessionRepo = sessionRepo;
    this.messageRepo = messageRepo;
    this.aiChatClientProvider = aiChatClientProvider;
    this.aiProperties = aiProperties;
    this.openAiStreamClient = openAiStreamClient;
    this.taskExecutor = taskExecutor;
    this.rateLimiter = rateLimiter;
    this.rateLimitProperties = rateLimitProperties;
  }

  @Transactional
  public AiChatCreateSessionResponse createSession(long userId, Long childId) {
    subscriptionService.requireSubscribed(userId);
    if (childId != null && childId <= 0) {
      throw new AppException(ErrorCode.INVALID_REQUEST, "childId must be positive");
    }
    var sessionId = sessionRepo.create(userId, childId);
    return new AiChatCreateSessionResponse(sessionId);
  }

  public List<AiChatSessionView> listSessions(long userId, int limit) {
    subscriptionService.requireSubscribed(userId);
    var safeLimit = Math.max(1, Math.min(100, limit));
    return sessionRepo.listByUser(userId, safeLimit).stream()
        .map(s -> new AiChatSessionView(s.id(), s.childId(), s.status(), s.lastActiveAt()))
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
        sessionRepo.findById(sessionId).orElseThrow(() -> new AppException(ErrorCode.NOT_FOUND, "session not found"));
    if (session.userId() != userId) {
      throw new AppException(ErrorCode.FORBIDDEN_RESOURCE, "forbidden");
    }
    if (!"ACTIVE".equalsIgnoreCase(session.status())) {
      throw new AppException(ErrorCode.INVALID_REQUEST, "session not active");
    }

    var msgId = messageRepo.insert(sessionId, userId, "user", content.trim());
    sessionRepo.touch(sessionId);
    return new AiChatMessageCreateResponse(msgId);
  }

  public SseEmitter streamAssistantReply(long userId, long sessionId) throws IOException {
    subscriptionService.requireSubscribed(userId);
    var session =
        sessionRepo.findById(sessionId).orElseThrow(() -> new AppException(ErrorCode.NOT_FOUND, "session not found"));
    if (session.userId() != userId) {
      throw new AppException(ErrorCode.FORBIDDEN_RESOURCE, "forbidden");
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
            if (aiProperties.mockEnabled()) {
              var ai = aiChatClientProvider.get();
              var reply = ai.chat(toChatMessages(context));
              if (reply == null) {
                reply = "";
              }
              reply = reply.trim();
              for (var chunk : chunk(reply, 24)) {
                replyBuilder.append(chunk);
                emitter.send(SseEmitter.event().name("delta").data(chunk, MediaType.TEXT_PLAIN));
              }
              emitter.send(SseEmitter.event().name("done").data("[DONE]", MediaType.TEXT_PLAIN));
            } else {
              var messages = new ArrayList<AiChatClient.ChatMessage>();
              messages.add(
                  new AiChatClient.ChatMessage(
                      "system",
                      "请使用 Markdown 格式输出（不要输出 HTML）。内容共情、具体、可执行，不做诊断、不贴标签。"));
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
            }

            var reply = replyBuilder.toString().trim();
            if (!reply.isBlank()) {
              messageRepo.insert(sessionId, userId, "assistant", reply);
              sessionRepo.touch(sessionId);
            }
            emitter.complete();
          } catch (Exception e) {
            try {
              emitter.send(SseEmitter.event().name("error").data("error", MediaType.TEXT_PLAIN));
            } catch (Exception ignored) {
            }
            emitter.completeWithError(e);
          }
        });
    return emitter;
  }

  private static List<AiChatClient.ChatMessage> toChatMessages(
      List<AiChatMessageRepository.AiChatMessageRow> rows) {
    return rows.stream()
        .map(r -> new AiChatClient.ChatMessage(r.role(), r.content()))
        .toList();
  }

  private static List<String> chunk(String text, int maxBytes) {
    if (text == null || text.isBlank()) {
      return List.of();
    }
    var out = new java.util.ArrayList<String>();
    var sb = new StringBuilder();
    int bytes = 0;
    for (int i = 0; i < text.length(); i++) {
      var ch = text.charAt(i);
      var b = String.valueOf(ch).getBytes(StandardCharsets.UTF_8).length;
      if (bytes + b > maxBytes && sb.length() > 0) {
        out.add(sb.toString());
        sb.setLength(0);
        bytes = 0;
      }
      sb.append(ch);
      bytes += b;
    }
    if (sb.length() > 0) {
      out.add(sb.toString());
    }
    return out;
  }
}
