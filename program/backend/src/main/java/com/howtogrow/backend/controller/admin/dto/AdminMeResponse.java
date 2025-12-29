package com.howtogrow.backend.controller.admin.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

public record AdminMeResponse(
    @Schema(description = "管理员ID") long adminUserId,
    @Schema(description = "用户名") String username,
    @Schema(description = "权限码列表（用于前端渲染）") List<String> permissionCodes) {}
