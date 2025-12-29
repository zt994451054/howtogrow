package com.howtogrow.backend.controller.miniprogram.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "创建 AI 对话会话请求")
public record AiChatCreateSessionRequest(@Schema(description = "关联孩子ID（可选）") Long childId) {}
