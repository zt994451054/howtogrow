package com.howtogrow.backend.controller.miniprogram.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "创建 AI 对话会话响应")
public record AiChatCreateSessionResponse(@Schema(description = "会话ID") long sessionId) {}
