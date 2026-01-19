package com.howtogrow.backend.controller.admin.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;

@Schema(description = "快捷问题（运营端）")
public record AiQuickQuestionView(
    @Schema(description = "ID") long id,
    @Schema(description = "快捷问题内容（同时用于展示与发送给 Agent）") String prompt,
    @Schema(description = "状态：1启用 0停用") int status,
    @Schema(description = "排序号（升序）") int sortNo,
    @Schema(description = "创建时间") Instant createdAt,
    @Schema(description = "更新时间") Instant updatedAt) {}

