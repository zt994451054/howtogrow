package com.howtogrow.backend.controller.miniprogram.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "鸡汤语响应")
public record QuoteResponse(@Schema(description = "鸡汤语内容") String content) {}
