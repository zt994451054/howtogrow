package com.howtogrow.backend.controller.miniprogram;

import com.howtogrow.backend.api.ApiResponse;
import com.howtogrow.backend.api.TraceId;
import com.howtogrow.backend.auth.AuthContext;
import com.howtogrow.backend.controller.miniprogram.dto.GrowthReportResponse;
import com.howtogrow.backend.service.miniprogram.GrowthReportService;
import io.swagger.v3.oas.annotations.Parameter;
import java.time.LocalDate;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/miniprogram/reports")
public class MiniprogramReportController {
  private final GrowthReportService reportService;

  public MiniprogramReportController(GrowthReportService reportService) {
    this.reportService = reportService;
  }

  @GetMapping("/growth")
  public ApiResponse<GrowthReportResponse> growth(
      @Parameter(description = "孩子ID") @RequestParam long childId,
      @Parameter(description = "开始日期（yyyy-MM-dd）")
          @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
      @Parameter(description = "结束日期（yyyy-MM-dd）")
          @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
    var user = AuthContext.requireMiniprogram();
    return ApiResponse.ok(reportService.growth(user.userId(), childId, from, to), TraceId.current());
  }
}
