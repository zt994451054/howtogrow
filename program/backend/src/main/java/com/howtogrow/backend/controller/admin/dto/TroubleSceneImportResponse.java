package com.howtogrow.backend.controller.admin.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "烦恼场景导入结果")
public record TroubleSceneImportResponse(@Schema(description = "成功导入条数") int imported) {}

