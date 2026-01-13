package com.howtogrow.backend.controller.miniprogram.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "上传头像响应")
public record UploadAvatarResponse(@Schema(description = "头像URL") String url) {}
