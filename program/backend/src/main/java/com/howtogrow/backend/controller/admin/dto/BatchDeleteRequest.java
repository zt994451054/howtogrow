package com.howtogrow.backend.controller.admin.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import java.util.List;

@Schema(description = "批量删除请求")
public record BatchDeleteRequest(
    @Schema(description = "ID 列表（去重由服务端处理）")
        @NotEmpty
        @Size(max = 2000)
        List<Long> ids) {}

