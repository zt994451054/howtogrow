package com.howtogrow.backend.controller.admin.dto;

import io.swagger.v3.oas.annotations.media.Schema;

public record QuoteView(
    @Schema(description = "鸡汤语ID") long id,
    @Schema(description = "内容") String content,
    @Schema(description = "状态：0禁用 1启用") int status) {}
