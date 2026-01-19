package com.howtogrow.backend.controller.admin;

import com.howtogrow.backend.api.ApiResponse;
import com.howtogrow.backend.api.TraceId;
import com.howtogrow.backend.controller.admin.dto.AiQuickQuestionUpsertRequest;
import com.howtogrow.backend.controller.admin.dto.AiQuickQuestionView;
import com.howtogrow.backend.controller.admin.dto.PageResponse;
import com.howtogrow.backend.service.admin.AdminAiQuickQuestionService;
import io.swagger.v3.oas.annotations.Parameter;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/admin/ai-quick-questions")
@com.howtogrow.backend.auth.AdminPermissionRequired({"AI_QUICK_QUESTION:MANAGE"})
public class AdminAiQuickQuestionController {
  private final AdminAiQuickQuestionService service;

  public AdminAiQuickQuestionController(AdminAiQuickQuestionService service) {
    this.service = service;
  }

  @GetMapping
  public ApiResponse<PageResponse<AiQuickQuestionView>> list(
      @Parameter(description = "页码（从1开始）") @RequestParam(defaultValue = "1") @Min(1) int page,
      @Parameter(description = "每页条数（1-200）") @RequestParam(defaultValue = "20") @Min(1) @Max(200) int pageSize,
      @Parameter(description = "状态：0停用 1启用") @RequestParam(required = false) @Min(0) @Max(1) Integer status,
      @Parameter(description = "关键字（prompt 模糊匹配）") @RequestParam(required = false) String keyword) {
    return ApiResponse.ok(service.list(page, pageSize, status, keyword), TraceId.current());
  }

  @PostMapping
  public ApiResponse<Long> create(@Valid @RequestBody AiQuickQuestionUpsertRequest request) {
    return ApiResponse.ok(service.create(request), TraceId.current());
  }

  @PutMapping("/{id}")
  public ApiResponse<Void> update(
      @Parameter(description = "快捷问题ID") @PathVariable long id, @Valid @RequestBody AiQuickQuestionUpsertRequest request) {
    service.update(id, request);
    return ApiResponse.ok(null, TraceId.current());
  }

  @DeleteMapping("/{id}")
  public ApiResponse<Void> delete(@Parameter(description = "快捷问题ID") @PathVariable long id) {
    service.delete(id);
    return ApiResponse.ok(null, TraceId.current());
  }
}

