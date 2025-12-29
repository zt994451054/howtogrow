package com.howtogrow.backend.controller.admin.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

public record RoleView(
    @Schema(description = "角色ID") long roleId,
    @Schema(description = "角色编码") String code,
    @Schema(description = "角色名称") String name,
    @Schema(description = "权限码列表") List<String> permissionCodes) {}
