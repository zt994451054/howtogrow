package com.howtogrow.backend.controller.miniprogram.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "AI 自测总结响应")
public record AiSummaryResponse(@Schema(description = "总结内容（<=70字）") String content) {}
