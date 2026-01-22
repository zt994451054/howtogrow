package com.howtogrow.backend.controller.admin;

import com.howtogrow.backend.api.ApiResponse;
import com.howtogrow.backend.api.TraceId;
import com.howtogrow.backend.controller.admin.dto.BatchDeleteRequest;
import com.howtogrow.backend.controller.admin.dto.PageResponse;
import com.howtogrow.backend.controller.admin.dto.QuoteCreateRequest;
import com.howtogrow.backend.controller.admin.dto.QuoteUpdateRequest;
import com.howtogrow.backend.controller.admin.dto.QuoteView;
import com.howtogrow.backend.service.admin.AdminQuoteService;
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
@RequestMapping("/api/v1/admin/quotes")
@com.howtogrow.backend.auth.AdminPermissionRequired({"QUOTE:MANAGE"})
public class AdminQuoteController {
  private final AdminQuoteService quoteService;

  public AdminQuoteController(AdminQuoteService quoteService) {
    this.quoteService = quoteService;
  }

  @GetMapping
  public ApiResponse<PageResponse<QuoteView>> list(
      @Parameter(description = "页码（从1开始）") @RequestParam(defaultValue = "1") @Min(1) int page,
      @Parameter(description = "每页条数（1-200）") @RequestParam(defaultValue = "20") @Min(1) @Max(200) int pageSize,
      @Parameter(description = "场景：每日觉察/育儿状态/烦恼档案/育儿日记") @RequestParam(required = false) String scene,
      @Parameter(description = "状态：0禁用 1启用") @RequestParam(required = false) @Min(0) @Max(1) Integer status,
      @Parameter(description = "关键字（content 模糊匹配）") @RequestParam(required = false) String keyword) {
    return ApiResponse.ok(quoteService.list(page, pageSize, scene, status, keyword), TraceId.current());
  }

  @PostMapping
  public ApiResponse<Void> create(@Valid @RequestBody QuoteCreateRequest request) {
    quoteService.create(request);
    return ApiResponse.ok(null, TraceId.current());
  }

  @PutMapping("/{id}")
  public ApiResponse<Void> update(
      @Parameter(description = "鸡汤语ID") @PathVariable long id,
      @Valid @RequestBody QuoteUpdateRequest request) {
    quoteService.update(id, request);
    return ApiResponse.ok(null, TraceId.current());
  }

  @DeleteMapping("/{id}")
  public ApiResponse<Void> delete(@Parameter(description = "鸡汤语ID") @PathVariable long id) {
    quoteService.delete(id);
    return ApiResponse.ok(null, TraceId.current());
  }

  @PostMapping("/batch-delete")
  public ApiResponse<Void> batchDelete(@Valid @RequestBody BatchDeleteRequest request) {
    quoteService.batchDelete(request.ids());
    return ApiResponse.ok(null, TraceId.current());
  }
}
