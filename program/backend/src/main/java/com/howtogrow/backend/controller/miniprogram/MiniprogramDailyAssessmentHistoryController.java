package com.howtogrow.backend.controller.miniprogram;

import com.howtogrow.backend.api.ApiResponse;
import com.howtogrow.backend.api.TraceId;
import com.howtogrow.backend.auth.AuthContext;
import com.howtogrow.backend.controller.miniprogram.dto.DailyAssessmentRecordDetailResponse;
import com.howtogrow.backend.controller.miniprogram.dto.DailyAssessmentRecordView;
import com.howtogrow.backend.service.miniprogram.DailyAssessmentHistoryService;
import io.swagger.v3.oas.annotations.Parameter;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/miniprogram/assessments/daily")
public class MiniprogramDailyAssessmentHistoryController {
  private final DailyAssessmentHistoryService historyService;

  public MiniprogramDailyAssessmentHistoryController(DailyAssessmentHistoryService historyService) {
    this.historyService = historyService;
  }

  @GetMapping("/records")
  public ApiResponse<List<DailyAssessmentRecordView>> list(
      @Parameter(description = "返回条数（默认 20，最大 100）") @RequestParam(defaultValue = "20") int limit,
      @Parameter(description = "偏移量（默认 0）") @RequestParam(defaultValue = "0") int offset) {
    var user = AuthContext.requireMiniprogram();
    return ApiResponse.ok(historyService.list(user.userId(), limit, offset), TraceId.current());
  }

  @GetMapping("/records/{assessmentId}")
  public ApiResponse<DailyAssessmentRecordDetailResponse> detail(
      @Parameter(description = "自测记录ID") @PathVariable long assessmentId) {
    var user = AuthContext.requireMiniprogram();
    return ApiResponse.ok(historyService.detail(user.userId(), assessmentId), TraceId.current());
  }
}

