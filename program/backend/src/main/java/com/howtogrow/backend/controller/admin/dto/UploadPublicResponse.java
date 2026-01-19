package com.howtogrow.backend.controller.admin.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "上传响应")
public record UploadPublicResponse(@Schema(description = "可访问URL") String url) {}

