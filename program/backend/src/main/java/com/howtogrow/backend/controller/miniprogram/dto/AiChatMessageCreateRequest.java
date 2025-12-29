package com.howtogrow.backend.controller.miniprogram.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

@Schema(description = "发送用户消息请求")
public record AiChatMessageCreateRequest(@Schema(description = "消息内容（Markdown/纯文本）") @NotBlank String content) {}
