package com.howtogrow.backend.controller.miniprogram.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "发送用户消息响应")
public record AiChatMessageCreateResponse(@Schema(description = "消息ID") long messageId) {}
