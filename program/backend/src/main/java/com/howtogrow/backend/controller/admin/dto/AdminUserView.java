package com.howtogrow.backend.controller.admin.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;
import java.util.List;

public record AdminUserView(
    @Schema(description = "管理员ID") long adminUserId,
    @Schema(description = "用户名") String username,
    @Schema(description = "状态：0禁用 1启用") int status,
    @Schema(description = "创建时间") Instant createdAt,
    @Schema(description = "角色编码列表") List<String> roleCodes) {}
