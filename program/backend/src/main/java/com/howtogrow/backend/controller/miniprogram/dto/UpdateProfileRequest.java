package com.howtogrow.backend.controller.miniprogram.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.NotBlank;
import java.time.LocalDate;

@Schema(description = "更新用户信息请求")
public record UpdateProfileRequest(
        @Schema(description = "用户昵称", requiredMode = Schema.RequiredMode.REQUIRED) @NotBlank String nickname,
        @Schema(description = "头像URL", requiredMode = Schema.RequiredMode.REQUIRED) @NotBlank String avatarUrl,
        @Schema(description = "出生日期（YYYY-MM-DD，可选）") @PastOrPresent LocalDate birthDate) {
}
