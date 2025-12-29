package com.howtogrow.backend.controller.admin;

import com.howtogrow.backend.api.ApiResponse;
import com.howtogrow.backend.api.TraceId;
import com.howtogrow.backend.auth.AuthContext;
import com.howtogrow.backend.controller.admin.dto.QuestionImportResponse;
import com.howtogrow.backend.service.admin.AdminQuestionImportService;
import io.swagger.v3.oas.annotations.Parameter;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/v1/admin/questions")
@com.howtogrow.backend.auth.AdminPermissionRequired({"QUESTION:IMPORT"})
public class AdminQuestionImportController {
  private final AdminQuestionImportService importService;

  public AdminQuestionImportController(AdminQuestionImportService importService) {
    this.importService = importService;
  }

  @PostMapping(value = "/import-excel", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
  public ApiResponse<QuestionImportResponse> importExcel(
      @Parameter(description = "Excel 文件") @RequestPart("file") MultipartFile file) {
    return ApiResponse.ok(importService.importExcel(file), TraceId.current());
  }
}
