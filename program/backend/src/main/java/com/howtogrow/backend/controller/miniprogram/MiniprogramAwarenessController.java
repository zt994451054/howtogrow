package com.howtogrow.backend.controller.miniprogram;

import com.howtogrow.backend.api.ApiResponse;
import com.howtogrow.backend.api.TraceId;
import com.howtogrow.backend.auth.AuthContext;
import com.howtogrow.backend.controller.miniprogram.dto.MonthlyAwarenessResponse;
import com.howtogrow.backend.service.miniprogram.MiniprogramAwarenessService;
import io.swagger.v3.oas.annotations.Parameter;
import jakarta.validation.constraints.Min;
import java.time.YearMonth;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/miniprogram/awareness")
public class MiniprogramAwarenessController {
  private final MiniprogramAwarenessService awarenessService;

  public MiniprogramAwarenessController(MiniprogramAwarenessService awarenessService) {
    this.awarenessService = awarenessService;
  }

  @GetMapping("/monthly")
  public ApiResponse<MonthlyAwarenessResponse> monthly(
      @Parameter(description = "孩子ID") @RequestParam @Min(1) long childId,
      @Parameter(description = "月份（YYYY-MM）") @RequestParam @DateTimeFormat(pattern = "yyyy-MM") YearMonth month) {
    var user = AuthContext.requireMiniprogram();
    return ApiResponse.ok(awarenessService.getMonthly(user.userId(), childId, month), TraceId.current());
  }
}

