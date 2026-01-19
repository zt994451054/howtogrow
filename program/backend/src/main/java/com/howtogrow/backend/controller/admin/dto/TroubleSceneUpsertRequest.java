package com.howtogrow.backend.controller.admin.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

@Schema(description = "烦恼场景 新增/更新请求")
public record TroubleSceneUpsertRequest(
    @Schema(description = "名称（未删除唯一）") @NotBlank String name,
    @Schema(description = "logo图片URL（可为空）") String logoUrl,
    @Schema(description = "适用最小年龄（整数，单位：岁，含边界）") @NotNull @Min(0) @Max(18) Integer minAge,
    @Schema(description = "适用最大年龄（整数，单位：岁，含边界）") @NotNull @Min(0) @Max(18) Integer maxAge) {}
