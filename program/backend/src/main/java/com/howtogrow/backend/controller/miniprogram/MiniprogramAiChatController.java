package com.howtogrow.backend.controller.miniprogram;

import com.howtogrow.backend.api.ApiResponse;
import com.howtogrow.backend.api.TraceId;
import com.howtogrow.backend.auth.AuthContext;
import com.howtogrow.backend.controller.miniprogram.dto.AiChatCreateSessionRequest;
import com.howtogrow.backend.controller.miniprogram.dto.AiChatCreateSessionResponse;
import com.howtogrow.backend.controller.miniprogram.dto.AiChatMessageCreateRequest;
import com.howtogrow.backend.controller.miniprogram.dto.AiChatMessageCreateResponse;
import com.howtogrow.backend.controller.miniprogram.dto.AiChatMessageView;
import com.howtogrow.backend.controller.miniprogram.dto.AiChatSessionView;
import com.howtogrow.backend.service.miniprogram.AiChatService;
import io.swagger.v3.oas.annotations.Parameter;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@RestController
@RequestMapping("/api/v1/miniprogram/ai/chat")
public class MiniprogramAiChatController {
  private final AiChatService chatService;

  public MiniprogramAiChatController(AiChatService chatService) {
    this.chatService = chatService;
  }

  @PostMapping("/sessions")
  public ApiResponse<AiChatCreateSessionResponse> createSession(
      @Valid @RequestBody AiChatCreateSessionRequest request) {
    var user = AuthContext.requireMiniprogram();
    return ApiResponse.ok(chatService.createSession(user.userId(), request.childId()), TraceId.current());
  }

  @GetMapping("/sessions")
  public ApiResponse<List<AiChatSessionView>> sessions(
      @Parameter(description = "返回会话数上限") @RequestParam(defaultValue = "20") int limit) {
    var user = AuthContext.requireMiniprogram();
    return ApiResponse.ok(chatService.listSessions(user.userId(), limit), TraceId.current());
  }

  @GetMapping("/quick-questions")
  public ApiResponse<List<String>> quickQuestions(
      @Parameter(description = "返回快捷问题数上限（默认 6，最大 20）") @RequestParam(defaultValue = "6") int limit) {
    var user = AuthContext.requireMiniprogram();
    return ApiResponse.ok(chatService.listQuickQuestions(user.userId(), limit), TraceId.current());
  }

  @GetMapping("/sessions/{sessionId}/messages")
  public ApiResponse<List<AiChatMessageView>> messages(
      @Parameter(description = "会话ID") @PathVariable long sessionId,
      @Parameter(description = "返回消息数上限（默认 20，最大 100）") @RequestParam(defaultValue = "20") int limit,
      @Parameter(description = "仅返回 messageId < beforeMessageId 的消息（用于分页）")
          @RequestParam(required = false)
          Long beforeMessageId) {
    var user = AuthContext.requireMiniprogram();
    return ApiResponse.ok(chatService.listMessages(user.userId(), sessionId, limit, beforeMessageId), TraceId.current());
  }

  @PostMapping("/sessions/{sessionId}/messages")
  public ApiResponse<AiChatMessageCreateResponse> createMessage(
      @Parameter(description = "会话ID") @PathVariable long sessionId,
      @Valid @RequestBody AiChatMessageCreateRequest request) {
    var user = AuthContext.requireMiniprogram();
    return ApiResponse.ok(chatService.createUserMessage(user.userId(), sessionId, request.content()), TraceId.current());
  }

  @GetMapping("/sessions/{sessionId}/stream")
  public ResponseEntity<SseEmitter> stream(@Parameter(description = "会话ID") @PathVariable long sessionId) {
    var user = AuthContext.requireMiniprogram();
    return ResponseEntity.ok()
        .contentType(MediaType.TEXT_EVENT_STREAM)
        .body(chatService.streamAssistantReply(user.userId(), sessionId));
  }
}
