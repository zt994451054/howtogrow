package com.howtogrow.backend.service.miniprogram;

import com.howtogrow.backend.api.ErrorCode;
import com.howtogrow.backend.api.exception.AppException;
import com.howtogrow.backend.controller.miniprogram.dto.DimensionScoreView;
import com.howtogrow.backend.controller.miniprogram.dto.GrowthReportResponse;
import com.howtogrow.backend.domain.capability.CapabilityDimension;
import com.howtogrow.backend.infrastructure.child.ChildRepository;
import com.howtogrow.backend.infrastructure.report.GrowthReportRepository;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;

@Service
public class GrowthReportService {
  private final ChildRepository childRepo;
  private final GrowthReportRepository reportRepo;

  public GrowthReportService(ChildRepository childRepo, GrowthReportRepository reportRepo) {
    this.childRepo = childRepo;
    this.reportRepo = reportRepo;
  }

  public GrowthReportResponse growth(long userId, long childId, LocalDate from, LocalDate to) {
    if (from == null || to == null || to.isBefore(from)) {
      throw new AppException(ErrorCode.INVALID_REQUEST, "invalid date range");
    }
    var child =
        childRepo.findById(childId).orElseThrow(() -> new AppException(ErrorCode.NOT_FOUND, "child not found"));
    if (child.userId() != userId) {
      throw new AppException(ErrorCode.FORBIDDEN_RESOURCE, "forbidden");
    }

    var rows = reportRepo.listDailyDimensionScores(userId, childId, from, to);
    Map<LocalDate, List<DimensionScoreView>> byDate = new LinkedHashMap<>();
    for (var row : rows) {
      byDate.computeIfAbsent(row.bizDate(), d -> new ArrayList<>())
          .add(
              new DimensionScoreView(
                  row.dimensionCode(),
                  CapabilityDimension.displayNameOf(row.dimensionCode()),
                  row.score()));
    }
    for (var scores : byDate.values()) {
      scores.sort(Comparator.comparingInt(s -> CapabilityDimension.sortNoOf(s.dimensionCode())));
    }

    var days =
        byDate.entrySet().stream()
            .map(e -> new GrowthReportResponse.GrowthDayView(e.getKey(), e.getValue()))
            .toList();
    return new GrowthReportResponse(childId, from, to, days);
  }
}
