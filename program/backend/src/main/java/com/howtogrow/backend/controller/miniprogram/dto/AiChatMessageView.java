package com.howtogrow.backend.controller.miniprogram.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;

@Schema(description = "AI 对话消息")
public record AiChatMessageView(
    @Schema(description = "消息ID") long messageId,
    @Schema(description = "角色：user/assistant/system") String role,
    @Schema(description = "消息内容（Markdown/纯文本）") String content,
    @Schema(description = "消息时间") Instant createdAt) {}

