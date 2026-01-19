package com.howtogrow.backend.controller.miniprogram;

import com.howtogrow.backend.api.ApiResponse;
import com.howtogrow.backend.api.TraceId;
import com.howtogrow.backend.auth.AuthContext;
import com.howtogrow.backend.controller.miniprogram.dto.DailyParentingDiaryUpsertRequest;
import com.howtogrow.backend.controller.miniprogram.dto.DailyParentingDiaryView;
import com.howtogrow.backend.service.miniprogram.MiniprogramDailyParentingDiaryService;
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
@RequestMapping("/api/v1/miniprogram/diary/daily")
public class MiniprogramDailyParentingDiaryController {
  private final MiniprogramDailyParentingDiaryService diaryService;

  public MiniprogramDailyParentingDiaryController(MiniprogramDailyParentingDiaryService diaryService) {
    this.diaryService = diaryService;
  }

  @GetMapping
  public ApiResponse<DailyParentingDiaryView> get(
      @Parameter(description = "孩子ID") @RequestParam @Min(1) long childId,
      @Parameter(description = "记录日期（YYYY-MM-DD；不传则默认今天）") @RequestParam(required = false) LocalDate recordDate) {
    var user = AuthContext.requireMiniprogram();
    return ApiResponse.ok(diaryService.get(user.userId(), childId, recordDate), TraceId.current());
  }

  @PutMapping
  public ApiResponse<Void> upsert(@Valid @RequestBody DailyParentingDiaryUpsertRequest request) {
    var user = AuthContext.requireMiniprogram();
    diaryService.upsert(user.userId(), request);
    return ApiResponse.ok(null, TraceId.current());
  }
}

