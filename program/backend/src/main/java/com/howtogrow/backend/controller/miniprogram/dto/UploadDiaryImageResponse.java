package com.howtogrow.backend.controller.miniprogram.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "上传育儿日记图片响应")
public record UploadDiaryImageResponse(@Schema(description = "图片 URL") String url) {}

