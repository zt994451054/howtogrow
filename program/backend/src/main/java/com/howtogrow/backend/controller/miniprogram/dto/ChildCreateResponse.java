package com.howtogrow.backend.controller.miniprogram.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "创建孩子响应")
public record ChildCreateResponse(@Schema(description = "孩子ID") long childId) {}
