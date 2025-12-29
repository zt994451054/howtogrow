package com.howtogrow.backend.controller.miniprogram.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;

@Schema(description = "创建孩子请求")
public record ChildCreateRequest(
    @Schema(description = "孩子昵称") @NotBlank String nickname,
    @Schema(description = "性别：0未知 1男 2女") @NotNull @Min(0) @Max(2) Integer gender,
    @Schema(description = "出生日期（YYYY-MM-DD）") @NotNull LocalDate birthDate) {}
