package com.howtogrow.backend.controller.admin;

import com.howtogrow.backend.api.ApiResponse;
import com.howtogrow.backend.api.TraceId;
import com.howtogrow.backend.controller.admin.dto.AssessmentDetailView;
import com.howtogrow.backend.controller.admin.dto.AssessmentView;
import com.howtogrow.backend.controller.admin.dto.PageResponse;
import com.howtogrow.backend.service.admin.AdminAssessmentService;
import io.swagger.v3.oas.annotations.Parameter;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/admin/assessments")
@com.howtogrow.backend.auth.AdminPermissionRequired({"ASSESSMENT:READ"})
public class AdminAssessmentController {
  private final AdminAssessmentService assessmentService;

  public AdminAssessmentController(AdminAssessmentService assessmentService) {
    this.assessmentService = assessmentService;
  }

  @GetMapping
  public ApiResponse<PageResponse<AssessmentView>> list(
      @Parameter(description = "页码（从1开始）") @RequestParam(defaultValue = "1") @Min(1) int page,
      @Parameter(description = "每页条数（1-200）") @RequestParam(defaultValue = "20") @Min(1) @Max(200) int pageSize,
      @Parameter(description = "提交日期起（yyyy-MM-dd，北京时间口径，可选）") @RequestParam(required = false)
          @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
          LocalDate bizDateFrom,
      @Parameter(description = "提交日期止（yyyy-MM-dd，北京时间口径，可选）") @RequestParam(required = false)
          @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
          LocalDate bizDateTo,
      @Parameter(description = "用户ID（可选）") @RequestParam(required = false) Long userId,
      @Parameter(description = "孩子ID（可选）") @RequestParam(required = false) Long childId,
      @Parameter(description = "用户/孩子昵称关键词（可选）") @RequestParam(required = false) String keyword) {
    return ApiResponse.ok(
        assessmentService.list(page, pageSize, userId, childId, keyword, bizDateFrom, bizDateTo),
        TraceId.current());
  }

  @GetMapping("/{assessmentId:\\d+}")
  public ApiResponse<AssessmentDetailView> detail(
      @Parameter(description = "自测ID") @PathVariable long assessmentId) {
    return ApiResponse.ok(assessmentService.detail(assessmentId), TraceId.current());
  }

  @GetMapping("/export-excel")
  public ResponseEntity<byte[]> exportExcel(
      @Parameter(description = "提交日期起（yyyy-MM-dd，北京时间口径，可选）") @RequestParam(required = false)
          @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
          LocalDate bizDateFrom,
      @Parameter(description = "提交日期止（yyyy-MM-dd，北京时间口径，可选）") @RequestParam(required = false)
          @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
          LocalDate bizDateTo,
      @Parameter(description = "用户ID（可选）") @RequestParam(required = false) Long userId,
      @Parameter(description = "孩子ID（可选）") @RequestParam(required = false) Long childId,
      @Parameter(description = "用户/孩子昵称关键词（可选）") @RequestParam(required = false) String keyword) {
    var bytes = assessmentService.exportExcel(userId, childId, keyword, bizDateFrom, bizDateTo);
    var filename = buildExportFilename(bizDateFrom, bizDateTo);
    var cd = ContentDisposition.attachment().filename(filename, StandardCharsets.UTF_8).build();
    return ResponseEntity.ok()
        .contentType(
            MediaType.parseMediaType(
                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
        .header(HttpHeaders.CONTENT_DISPOSITION, cd.toString())
        .header(HttpHeaders.CACHE_CONTROL, "no-store")
        .body(bytes);
  }

  @GetMapping("/{assessmentId:\\d+}/export-word")
  public ResponseEntity<byte[]> exportWord(
      @Parameter(description = "自测ID") @PathVariable long assessmentId) {
    var bytes = assessmentService.exportWord(assessmentId);
    var filename = "自测结果_" + assessmentId + ".docx";
    var cd = ContentDisposition.attachment().filename(filename, StandardCharsets.UTF_8).build();
    return ResponseEntity.ok()
        .contentType(
            MediaType.parseMediaType(
                "application/vnd.openxmlformats-officedocument.wordprocessingml.document"))
        .header(HttpHeaders.CONTENT_DISPOSITION, cd.toString())
        .header(HttpHeaders.CACHE_CONTROL, "no-store")
        .body(bytes);
  }

  private static String buildExportFilename(LocalDate bizDateFrom, LocalDate bizDateTo) {
    if (bizDateFrom != null && bizDateTo != null) {
      return "自测记录_" + bizDateFrom + "_" + bizDateTo + ".xlsx";
    }
    if (bizDateFrom != null) {
      return "自测记录_" + bizDateFrom + ".xlsx";
    }
    if (bizDateTo != null) {
      return "自测记录_截至" + bizDateTo + ".xlsx";
    }
    return "自测记录.xlsx";
  }
}
