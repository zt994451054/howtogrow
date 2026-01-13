package com.howtogrow.backend.controller.miniprogram.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;

@Schema(description = "AI 对话会话信息")
public record AiChatSessionView(
    @Schema(description = "会话ID") long sessionId,
    @Schema(description = "关联孩子ID（可选）") Long childId,
    @Schema(description = "会话标题（可选）") String title,
    @Schema(description = "状态：ACTIVE/CLOSED") String status,
    @Schema(description = "最后活跃时间") Instant lastActiveAt) {}
