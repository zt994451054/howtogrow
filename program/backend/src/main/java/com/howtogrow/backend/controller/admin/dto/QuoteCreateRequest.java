package com.howtogrow.backend.controller.admin.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record QuoteCreateRequest(
    @Schema(description = "内容") @NotBlank String content,
    @Schema(description = "状态：0禁用 1启用") @NotNull @Min(0) @Max(1) Integer status) {}
