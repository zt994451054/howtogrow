package com.howtogrow.backend.controller.admin.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

public record PageResponse<T>(
    @Schema(description = "页码（从1开始）") int page,
    @Schema(description = "每页条数") int pageSize,
    @Schema(description = "总数") long total,
    @Schema(description = "数据列表") List<T> items) {}
