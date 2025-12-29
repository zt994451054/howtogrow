package com.howtogrow.backend.controller.admin.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import java.util.List;

public record AdminUserRoleUpdateRequest(
    @Schema(description = "角色编码列表") @NotNull List<String> roleCodes) {}
