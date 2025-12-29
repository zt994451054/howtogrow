package com.howtogrow.backend.controller.miniprogram;

import com.howtogrow.backend.api.ApiResponse;
import com.howtogrow.backend.api.TraceId;
import com.howtogrow.backend.auth.AuthContext;
import com.howtogrow.backend.controller.miniprogram.dto.AiSummaryResponse;
import com.howtogrow.backend.service.miniprogram.AiSummaryService;
import io.swagger.v3.oas.annotations.Parameter;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/miniprogram/assessments/daily")
public class MiniprogramAiController {
  private final AiSummaryService aiSummaryService;

  public MiniprogramAiController(AiSummaryService aiSummaryService) {
    this.aiSummaryService = aiSummaryService;
  }

  @PostMapping("/{assessmentId}/ai-summary")
  public ApiResponse<AiSummaryResponse> generateAiSummary(
      @Parameter(description = "自测ID") @PathVariable long assessmentId) {
    var user = AuthContext.requireMiniprogram();
    return ApiResponse.ok(aiSummaryService.generate(user.userId(), assessmentId), TraceId.current());
  }
}
