package com.howtogrow.backend.controller.admin.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

@Schema(description = "快捷问题新增/更新请求")
public record AiQuickQuestionUpsertRequest(
    @Schema(description = "快捷问题内容（同时用于展示与发送给 Agent）") @NotBlank @Size(max = 512) String prompt,
    @Schema(description = "状态：1启用 0停用") @NotNull @Min(0) @Max(1) Integer status,
    @Schema(description = "排序号（升序）") @NotNull Integer sortNo) {}

