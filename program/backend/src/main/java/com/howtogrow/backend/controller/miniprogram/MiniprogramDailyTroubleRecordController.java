package com.howtogrow.backend.controller.miniprogram;

import com.howtogrow.backend.api.ApiResponse;
import com.howtogrow.backend.api.TraceId;
import com.howtogrow.backend.auth.AuthContext;
import com.howtogrow.backend.controller.miniprogram.dto.DailyTroubleRecordUpsertRequest;
import com.howtogrow.backend.controller.miniprogram.dto.DailyTroubleRecordView;
import com.howtogrow.backend.service.miniprogram.MiniprogramDailyTroubleRecordService;
import io.swagger.v3.oas.annotations.Parameter;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import java.time.LocalDate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/miniprogram/troubles/daily")
public class MiniprogramDailyTroubleRecordController {
  private final MiniprogramDailyTroubleRecordService recordService;

  public MiniprogramDailyTroubleRecordController(MiniprogramDailyTroubleRecordService recordService) {
    this.recordService = recordService;
  }

  @GetMapping
  public ApiResponse<DailyTroubleRecordView> get(
      @Parameter(description = "孩子ID") @RequestParam @Min(1) long childId,
      @Parameter(description = "记录日期（YYYY-MM-DD；不传则默认今天）") @RequestParam(required = false) LocalDate recordDate) {
    var user = AuthContext.requireMiniprogram();
    return ApiResponse.ok(recordService.get(user.userId(), childId, recordDate), TraceId.current());
  }

  @PutMapping
  public ApiResponse<Void> upsert(@Valid @RequestBody DailyTroubleRecordUpsertRequest request) {
    var user = AuthContext.requireMiniprogram();
    recordService.upsert(user.userId(), request);
    return ApiResponse.ok(null, TraceId.current());
  }
}

