package com.howtogrow.backend.controller.admin.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.List;

@Schema(description = "批量更新题目关联烦恼场景请求")
public record BatchUpdateQuestionTroubleScenesRequest(
    @Schema(description = "题目ID列表（去重由服务端处理）")
        @NotEmpty
        @Size(max = 2000)
        List<Long> ids,
    @Schema(description = "烦恼场景ID列表（REPLACE 为空表示清空；APPEND 为空不合法）")
        @Size(max = 200)
        List<Long> troubleSceneIds,
    @Schema(description = "更新模式：APPEND 追加；REPLACE 覆盖（为空表示清空）")
        @NotNull
        TroubleSceneUpdateMode mode) {}

