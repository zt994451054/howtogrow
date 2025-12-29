package com.howtogrow.backend.controller.admin.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

public record AdminLoginRequest(
    @Schema(description = "用户名") @NotBlank String username,
    @Schema(description = "密码（明文）") @NotBlank String password) {}
