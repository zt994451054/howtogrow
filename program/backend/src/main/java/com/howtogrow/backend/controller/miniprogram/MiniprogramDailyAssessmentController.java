package com.howtogrow.backend.controller.miniprogram;

import com.howtogrow.backend.api.ApiResponse;
import com.howtogrow.backend.api.TraceId;
import com.howtogrow.backend.auth.AuthContext;
import com.howtogrow.backend.controller.miniprogram.dto.DailyAssessmentBeginRequest;
import com.howtogrow.backend.controller.miniprogram.dto.DailyAssessmentBeginResponse;
import com.howtogrow.backend.controller.miniprogram.dto.DailyAssessmentReplaceRequest;
import com.howtogrow.backend.controller.miniprogram.dto.DailyAssessmentReplaceResponse;
import com.howtogrow.backend.controller.miniprogram.dto.DailyAssessmentSubmitRequest;
import com.howtogrow.backend.controller.miniprogram.dto.DailyAssessmentSubmitResponse;
import com.howtogrow.backend.service.miniprogram.DailyAssessmentService;
import io.swagger.v3.oas.annotations.Parameter;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/miniprogram/assessments/daily")
public class MiniprogramDailyAssessmentController {
  private final DailyAssessmentService assessmentService;

  public MiniprogramDailyAssessmentController(DailyAssessmentService assessmentService) {
    this.assessmentService = assessmentService;
  }

  @PostMapping("/begin")
  public ApiResponse<DailyAssessmentBeginResponse> begin(@Valid @RequestBody DailyAssessmentBeginRequest request) {
    var user = AuthContext.requireMiniprogram();
    return ApiResponse.ok(assessmentService.begin(user.userId(), request.childId()), TraceId.current());
  }

  @PostMapping("/sessions/{sessionId}/replace")
  public ApiResponse<DailyAssessmentReplaceResponse> replace(
      @Parameter(description = "自测会话ID") @PathVariable String sessionId,
      @Valid @RequestBody DailyAssessmentReplaceRequest request) {
    var user = AuthContext.requireMiniprogram();
    return ApiResponse.ok(
        assessmentService.replace(user.userId(), sessionId, request.childId(), request.displayOrder()),
        TraceId.current());
  }

  @PostMapping("/sessions/{sessionId}/submit")
  public ApiResponse<DailyAssessmentSubmitResponse> submit(
      @Parameter(description = "自测会话ID") @PathVariable String sessionId,
      @Valid @RequestBody DailyAssessmentSubmitRequest request) {
    var user = AuthContext.requireMiniprogram();
    return ApiResponse.ok(
        assessmentService.submit(user.userId(), sessionId, request),
        TraceId.current());
  }
}
