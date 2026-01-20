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
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import org.springframework.stereotype.Service;

@Service
public class GrowthReportService {
  private static final int WINDOW_SIZE = 5;
  private final ChildRepository childRepo;
  private final GrowthReportRepository reportRepo;

  public GrowthReportService(ChildRepository childRepo, GrowthReportRepository reportRepo) {
    this.childRepo = childRepo;
    this.reportRepo = reportRepo;
  }

  public GrowthReportResponse growth(long userId, long childId, LocalDate from, LocalDate to) {
    if (from == null || to == null || to.isBefore(from)) {
      throw new AppException(ErrorCode.INVALID_REQUEST, "日期范围不合法");
    }
    var child =
        childRepo.findById(childId).orElseThrow(() -> new AppException(ErrorCode.NOT_FOUND, "孩子不存在"));
    if (child.userId() != userId) {
      throw new AppException(ErrorCode.FORBIDDEN_RESOURCE, "无权限");
    }

    var rows = reportRepo.listDailyDimensionScores(userId, childId, from, to);
    var dimensions = resolveOrderedDimensions(rows);
    var dailyScores = toDailyScores(rows);
    var days = aggregateByWindow(dailyScores, dimensions);
    return new GrowthReportResponse(childId, from, to, days);
  }

  private static List<String> resolveOrderedDimensions(List<GrowthReportRepository.GrowthRow> rows) {
    Set<String> codes = new LinkedHashSet<>();
    for (var dimension : CapabilityDimension.ordered()) {
      codes.add(dimension.code());
    }
    for (var row : rows) {
      codes.add(row.dimensionCode());
    }
    return codes.stream()
        .sorted(
            Comparator.comparingInt((String code) -> CapabilityDimension.sortNoOf(code))
                .thenComparing(String::compareTo))
        .toList();
  }

  private static Map<LocalDate, Map<String, Long>> toDailyScores(
      List<GrowthReportRepository.GrowthRow> rows) {
    Map<LocalDate, Map<String, Long>> dailyScores = new TreeMap<>();
    for (var row : rows) {
      dailyScores
          .computeIfAbsent(row.bizDate(), d -> new HashMap<>())
          .merge(row.dimensionCode(), row.score(), Long::sum);
    }
    return dailyScores;
  }

  private static List<GrowthReportResponse.GrowthDayView> aggregateByWindow(
      Map<LocalDate, Map<String, Long>> dailyScores, List<String> orderedDimensions) {
    var dates = new ArrayList<>(dailyScores.keySet());
    List<GrowthReportResponse.GrowthDayView> days = new ArrayList<>();
    for (int startIndex = 0; startIndex < dates.size(); startIndex += WINDOW_SIZE) {
      int endIndexExclusive = Math.min(startIndex + WINDOW_SIZE, dates.size());
      var windowDates = dates.subList(startIndex, endIndexExclusive);

      var windowSize = windowDates.size();
      var outputDate = windowDates.get(windowSize - 1);

      List<DimensionScoreView> dimensionScores = new ArrayList<>(orderedDimensions.size());
      for (var dimensionCode : orderedDimensions) {
        long sum = 0;
        for (var bizDate : windowDates) {
          var dayScores = dailyScores.getOrDefault(bizDate, Map.of());
          sum += dayScores.getOrDefault(dimensionCode, 0L);
        }
        long avgScore = sum / windowSize;
        dimensionScores.add(
            new DimensionScoreView(
                dimensionCode, CapabilityDimension.displayNameOf(dimensionCode), avgScore));
      }

      dimensionScores.sort(
          Comparator.comparingInt((DimensionScoreView s) -> CapabilityDimension.sortNoOf(s.dimensionCode()))
              .thenComparing(DimensionScoreView::dimensionCode));
      days.add(new GrowthReportResponse.GrowthDayView(outputDate, dimensionScores));
    }
    return days;
  }
}
