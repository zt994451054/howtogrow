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
import com.howtogrow.backend.infrastructure.aichat.AiQuickQuestionRepository;
import com.howtogrow.backend.infrastructure.ai.AiChatClient;
import com.howtogrow.backend.infrastructure.ai.OpenAiStreamClient;
import com.howtogrow.backend.config.RateLimitProperties;
import com.howtogrow.backend.service.common.FixedWindowRateLimiter;
import com.howtogrow.backend.service.common.SubscriptionService;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
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
      你是家长的专属家庭教育指导师，你的任务是随时解答家长提出的任何关于教育孩子的问题。
      其他与教育孩子无关的问题不必解答。你解答的语气，要用朋友聊天的语气和口吻。
      回答的内容，不必出现太多的格式、标题之类的，都用正常聊天的方式呈现，针对家长的问题，最后要给出直接可执行的方法。
      输出格式要求：始终使用 Markdown 格式回复。
      """
          .trim();

  private final SubscriptionService subscriptionService;
  private final AiChatSessionRepository sessionRepo;
  private final AiChatMessageRepository messageRepo;
  private final AiQuickQuestionRepository quickQuestionRepo;
  private final OpenAiStreamClient openAiStreamClient;
  private final ObjectMapper objectMapper;
  private final TaskExecutor taskExecutor;
  private final FixedWindowRateLimiter rateLimiter;
  private final RateLimitProperties rateLimitProperties;

  public AiChatService(
      SubscriptionService subscriptionService,
      AiChatSessionRepository sessionRepo,
      AiChatMessageRepository messageRepo,
      AiQuickQuestionRepository quickQuestionRepo,
      OpenAiStreamClient openAiStreamClient,
      ObjectMapper objectMapper,
      TaskExecutor taskExecutor,
      FixedWindowRateLimiter rateLimiter,
      RateLimitProperties rateLimitProperties) {
    this.subscriptionService = subscriptionService;
    this.sessionRepo = sessionRepo;
    this.messageRepo = messageRepo;
    this.quickQuestionRepo = quickQuestionRepo;
    this.openAiStreamClient = openAiStreamClient;
    this.objectMapper = objectMapper;
    this.taskExecutor = taskExecutor;
    this.rateLimiter = rateLimiter;
    this.rateLimitProperties = rateLimitProperties;
  }

  public List<String> listQuickQuestions(long userId, int limit) {
    var safeLimit = Math.max(1, Math.min(20, limit));
    return quickQuestionRepo.listActivePrompts(safeLimit).stream()
        .map(AiChatService::safeText)
        .filter(s -> s != null)
        .toList();
  }

  @Transactional
  public AiChatCreateSessionResponse createSession(long userId, Long childId) {
    if (childId != null && childId <= 0) {
      throw new AppException(ErrorCode.INVALID_REQUEST, "childId 必须为正数");
    }
    var sessionId = sessionRepo.create(userId, childId);
    return new AiChatCreateSessionResponse(sessionId);
  }

  public List<AiChatSessionView> listSessions(long userId, int limit) {
    var safeLimit = Math.max(1, Math.min(100, limit));
    return sessionRepo.listByUser(userId, safeLimit).stream()
        .map(s -> new AiChatSessionView(s.id(), s.childId(), s.title(), s.status(), s.lastActiveAt()))
        .toList();
  }

  public List<AiChatMessageView> listMessages(long userId, long sessionId, int limit, Long beforeMessageId) {
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
    if (!subscriptionService.isSubscribed(userId) && messageRepo.existsAssistantMessageByUser(userId)) {
      throw new AppException(ErrorCode.SUBSCRIPTION_REQUIRED, "未订阅或已过期，请先开通会员");
    }
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
    var session =
        sessionRepo.findById(sessionId).orElseThrow(() -> new AppException(ErrorCode.NOT_FOUND, "会话不存在"));
    if (session.userId() != userId) {
      throw new AppException(ErrorCode.FORBIDDEN_RESOURCE, "无权限");
    }

    var emitter = new SseEmitter(120_000L);
    var latest = messageRepo.listPageDesc(sessionId, 1, null).stream().findFirst().orElse(null);
    if (latest == null || !"user".equalsIgnoreCase(latest.role())) {
      emitter.complete();
      return emitter;
    }
    var context = messageRepo.listRecent(sessionId, CONTEXT_LIMIT);
    if (context.isEmpty()) {
      emitter.complete();
      return emitter;
    }

    var streamWritable = new AtomicBoolean(true);
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
                  replyBuilder.append(delta);
                  if (!streamWritable.get()) {
                    return;
                  }
                  try {
                    emitter.send(SseEmitter.event().name("delta").data(delta, MediaType.TEXT_PLAIN));
                  } catch (Exception ignored) {
                    streamWritable.set(false);
                  }
                },
                done -> {
                  if (!streamWritable.get()) {
                    return;
                  }
                  try {
                    emitter.send(SseEmitter.event().name("done").data("[DONE]", MediaType.TEXT_PLAIN));
                  } catch (Exception ignored) {
                    streamWritable.set(false);
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

  private static String safeText(String text) {
    if (text == null) return null;
    var t = text.trim();
    return t.isBlank() ? null : t;
  }
}
