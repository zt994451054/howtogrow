package com.howtogrow.backend.controller.admin.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "烦恼场景更新模式")
public enum TroubleSceneUpdateMode {
  @Schema(description = "追加：在原有关联上增加（去重）")
  APPEND,

  @Schema(description = "覆盖：用新列表替换（为空表示清空）")
  REPLACE
}

