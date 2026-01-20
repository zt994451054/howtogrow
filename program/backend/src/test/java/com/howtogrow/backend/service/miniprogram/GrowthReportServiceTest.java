package com.howtogrow.backend.service.miniprogram;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.howtogrow.backend.controller.miniprogram.dto.GrowthReportResponse;
import com.howtogrow.backend.domain.capability.CapabilityDimension;
import com.howtogrow.backend.infrastructure.child.Child;
import com.howtogrow.backend.infrastructure.child.ChildRepository;
import com.howtogrow.backend.infrastructure.report.GrowthReportRepository;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class GrowthReportServiceTest {
  @Test
  void growth_aggregatesByFiveDataDays_fromStartDate() {
    var childRepo = mock(ChildRepository.class);
    var reportRepo = mock(GrowthReportRepository.class);
    var service = new GrowthReportService(childRepo, reportRepo);

    long userId = 1L;
    long childId = 2L;
    when(childRepo.findById(childId))
        .thenReturn(Optional.of(new Child(childId, userId, "n", 1, LocalDate.of(2020, 1, 1), "妈妈")));

    var d1 = LocalDate.of(2026, 1, 1);
    var d2 = LocalDate.of(2026, 1, 3);
    var d3 = LocalDate.of(2026, 1, 7);
    var d4 = LocalDate.of(2026, 1, 8);
    var d5 = LocalDate.of(2026, 1, 12);
    var d6 = LocalDate.of(2026, 1, 20);

    var rows =
        List.of(
            // Group 1: 5 data days -> output date is d5
            new GrowthReportRepository.GrowthRow(d1, CapabilityDimension.EMOTION_MANAGEMENT.code(), 10),
            new GrowthReportRepository.GrowthRow(d2, CapabilityDimension.EMOTION_MANAGEMENT.code(), 20),
            // Missing EMOTION_MANAGEMENT at d3 -> should be treated as 0
            new GrowthReportRepository.GrowthRow(d4, CapabilityDimension.EMOTION_MANAGEMENT.code(), 40),
            new GrowthReportRepository.GrowthRow(d5, CapabilityDimension.EMOTION_MANAGEMENT.code(), 50),

            // Another dimension exists only on two days in group 1
            new GrowthReportRepository.GrowthRow(d1, CapabilityDimension.LEARNING_SUPPORT.code(), 5),
            // d3 exists as a data day but EMOTION_MANAGEMENT is missing
            new GrowthReportRepository.GrowthRow(d3, CapabilityDimension.LEARNING_SUPPORT.code(), 0),
            new GrowthReportRepository.GrowthRow(d5, CapabilityDimension.LEARNING_SUPPORT.code(), 15),

            // Group 2: 1 data day -> output date is d6
            new GrowthReportRepository.GrowthRow(d6, CapabilityDimension.EMOTION_MANAGEMENT.code(), 60));

    when(reportRepo.listDailyDimensionScores(userId, childId, d1, d6)).thenReturn(rows);

    GrowthReportResponse resp = service.growth(userId, childId, d1, d6);
    assertEquals(childId, resp.childId());
    assertEquals(d1, resp.from());
    assertEquals(d6, resp.to());
    assertEquals(2, resp.days().size());

    var first = resp.days().get(0);
    assertEquals(d5, first.bizDate());
    // 10 + 20 + 0 + 40 + 50 = 120; avg over 5 data days => 24
    assertEquals(
        24L,
        scoreOf(first, CapabilityDimension.EMOTION_MANAGEMENT.code()),
        "emotion avg should include missing day as 0 and use integer division");
    // 5 + 0 + 0 + 0 + 15 = 20; avg => 4
    assertEquals(4L, scoreOf(first, CapabilityDimension.LEARNING_SUPPORT.code()));

    var second = resp.days().get(1);
    assertEquals(d6, second.bizDate());
    assertEquals(60L, scoreOf(second, CapabilityDimension.EMOTION_MANAGEMENT.code()));
  }

  private static long scoreOf(GrowthReportResponse.GrowthDayView day, String dimensionCode) {
    return day.dimensionScores().stream()
        .filter(s -> s.dimensionCode().equals(dimensionCode))
        .findFirst()
        .orElseThrow()
        .score();
  }
}
