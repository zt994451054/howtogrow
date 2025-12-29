package com.howtogrow.backend.controller.admin.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.util.List;

public record AdminUserCreateRequest(
    @Schema(description = "用户名") @NotBlank String username,
    @Schema(description = "初始密码（明文）") @NotBlank String password,
    @Schema(description = "角色编码列表") @NotNull List<String> roleCodes) {}
