package com.howtogrow.backend.controller.admin.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import java.util.List;

public record RolePermissionUpdateRequest(
    @Schema(description = "权限码列表") @NotNull List<String> permissionCodes) {}
