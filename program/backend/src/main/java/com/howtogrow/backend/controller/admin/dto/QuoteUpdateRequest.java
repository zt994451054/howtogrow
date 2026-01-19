package com.howtogrow.backend.controller.admin.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record QuoteUpdateRequest(
    @Schema(description = "内容") @NotBlank String content,
    @Schema(description = "场景：每日觉察/育儿状态/烦恼档案/育儿日记") @NotBlank String scene,
    @Schema(description = "适用最小年龄（整数，单位：岁，含边界）") @NotNull @Min(0) @Max(18) Integer minAge,
    @Schema(description = "适用最大年龄（整数，单位：岁，含边界）") @NotNull @Min(0) @Max(18) Integer maxAge,
    @Schema(description = "状态：0禁用 1启用") @NotNull @Min(0) @Max(1) Integer status) {}
