package com.howtogrow.backend.controller.admin.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

public record RoleCreateRequest(
    @Schema(description = "角色编码（唯一）") @NotBlank String code,
    @Schema(description = "角色名称") @NotBlank String name) {}
