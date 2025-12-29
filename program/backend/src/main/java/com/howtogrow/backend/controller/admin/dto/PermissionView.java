package com.howtogrow.backend.controller.admin.dto;

import io.swagger.v3.oas.annotations.media.Schema;

public record PermissionView(
    @Schema(description = "权限ID") long permissionId,
    @Schema(description = "权限码") String code,
    @Schema(description = "权限名称") String name) {}
