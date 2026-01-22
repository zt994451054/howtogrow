package com.howtogrow.backend.controller.admin;

import com.howtogrow.backend.api.ApiResponse;
import com.howtogrow.backend.api.TraceId;
import com.howtogrow.backend.controller.admin.dto.BatchDeleteRequest;
import com.howtogrow.backend.controller.admin.dto.PageResponse;
import com.howtogrow.backend.controller.admin.dto.QuestionDetailView;
import com.howtogrow.backend.controller.admin.dto.QuestionSummaryView;
import com.howtogrow.backend.controller.admin.dto.QuestionUpsertRequest;
import com.howtogrow.backend.service.admin.AdminQuestionService;
import com.howtogrow.backend.service.admin.AdminQuestionWriteService;
import io.swagger.v3.oas.annotations.Parameter;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/admin/questions")
@com.howtogrow.backend.auth.AdminPermissionRequired({"QUESTION:MANAGE"})
public class AdminQuestionController {
  private final AdminQuestionService questionService;
  private final AdminQuestionWriteService questionWriteService;

  public AdminQuestionController(
      AdminQuestionService questionService, AdminQuestionWriteService questionWriteService) {
    this.questionService = questionService;
    this.questionWriteService = questionWriteService;
  }

  @GetMapping
  public ApiResponse<PageResponse<QuestionSummaryView>> list(
      @Parameter(description = "年龄（岁，可选）") @RequestParam(required = false) Integer ageYear,
      @Parameter(description = "页码（从1开始）") @RequestParam(defaultValue = "1") @Min(1) int page,
      @Parameter(description = "每页条数（1-200）") @RequestParam(defaultValue = "20") @Min(1) @Max(200) int pageSize) {
    return ApiResponse.ok(questionService.list(ageYear, page, pageSize), TraceId.current());
  }

  @GetMapping("/{questionId}")
  public ApiResponse<QuestionDetailView> detail(@Parameter(description = "题目ID") @PathVariable long questionId) {
    return ApiResponse.ok(questionService.detail(questionId), TraceId.current());
  }

  @PostMapping
  public ApiResponse<Long> create(@Valid @RequestBody QuestionUpsertRequest request) {
    return ApiResponse.ok(questionWriteService.create(request), TraceId.current());
  }

  @PutMapping("/{questionId}")
  public ApiResponse<Void> update(
      @Parameter(description = "题目ID") @PathVariable long questionId,
      @Valid @RequestBody QuestionUpsertRequest request) {
    questionWriteService.update(questionId, request);
    return ApiResponse.ok(null, TraceId.current());
  }

  @DeleteMapping("/{questionId}")
  public ApiResponse<Void> delete(@Parameter(description = "题目ID") @PathVariable long questionId) {
    questionWriteService.delete(questionId);
    return ApiResponse.ok(null, TraceId.current());
  }

  @PostMapping("/batch-delete")
  public ApiResponse<Void> batchDelete(@Valid @RequestBody BatchDeleteRequest request) {
    questionWriteService.batchDelete(request.ids());
    return ApiResponse.ok(null, TraceId.current());
  }
}
