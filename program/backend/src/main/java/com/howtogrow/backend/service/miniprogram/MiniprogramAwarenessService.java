package com.howtogrow.backend.service.miniprogram;

import com.howtogrow.backend.api.ErrorCode;
import com.howtogrow.backend.api.exception.AppException;
import com.howtogrow.backend.controller.miniprogram.dto.MonthlyAwarenessResponse;
import com.howtogrow.backend.domain.parenting.ParentingStatus;
import com.howtogrow.backend.domain.time.BizClock;
import com.howtogrow.backend.infrastructure.assessment.DailyAssessmentHistoryRepository;
import com.howtogrow.backend.infrastructure.child.ChildRepository;
import com.howtogrow.backend.infrastructure.parenting.DailyParentingDiaryRepository;
import com.howtogrow.backend.infrastructure.parenting.DailyParentingStatusRepository;
import com.howtogrow.backend.infrastructure.trouble.DailyTroubleRecordRepository;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;

@Service
public class MiniprogramAwarenessService {
  private static final YearMonth MIN_MONTH = YearMonth.of(2025, 6);
  private static final ZoneId CN = ZoneId.of("Asia/Shanghai");
  private static final DateTimeFormatter ISO_OFFSET = DateTimeFormatter.ISO_OFFSET_DATE_TIME;

  private final BizClock bizClock;
  private final ChildRepository childRepo;
  private final DailyParentingStatusRepository parentingStatusRepo;
  private final DailyTroubleRecordRepository troubleRepo;
  private final DailyAssessmentHistoryRepository assessmentHistoryRepo;
  private final DailyParentingDiaryRepository diaryRepo;

  public MiniprogramAwarenessService(
      BizClock bizClock,
      ChildRepository childRepo,
      DailyParentingStatusRepository parentingStatusRepo,
      DailyTroubleRecordRepository troubleRepo,
      DailyAssessmentHistoryRepository assessmentHistoryRepo,
      DailyParentingDiaryRepository diaryRepo) {
    this.bizClock = bizClock;
    this.childRepo = childRepo;
    this.parentingStatusRepo = parentingStatusRepo;
    this.troubleRepo = troubleRepo;
    this.assessmentHistoryRepo = assessmentHistoryRepo;
    this.diaryRepo = diaryRepo;
  }

  public MonthlyAwarenessResponse getMonthly(long userId, long childId, YearMonth month) {
    if (month == null) {
      throw new AppException(ErrorCode.INVALID_REQUEST, "month 必填（YYYY-MM）");
    }
    if (month.isBefore(MIN_MONTH)) {
      throw new AppException(ErrorCode.INVALID_REQUEST, "month 不能早于 2025-06");
    }
    var today = bizClock.today();
    var current = YearMonth.from(today);
    if (month.isAfter(current)) {
      throw new AppException(ErrorCode.INVALID_REQUEST, "month 不能晚于当前月份");
    }

    var child =
        childRepo.findById(childId).orElseThrow(() -> new AppException(ErrorCode.NOT_FOUND, "孩子不存在"));
    if (child.userId() != userId) {
      throw new AppException(ErrorCode.FORBIDDEN_RESOURCE, "无权限");
    }

    LocalDate start = month.atDay(1);
    LocalDate end = month.equals(current) ? today : month.atEndOfMonth();

    var statusByDate = parentingStatusRepo.findStatusCodeByDayBetween(userId, childId, start, end);
    var troubleScenesByDate = troubleRepo.listActiveScenesByDayBetween(userId, childId, start, end);
    var diaries = diaryRepo.listByUserChildBetween(userId, childId, start, end);
    var diaryByDate = new HashMap<LocalDate, DailyParentingDiaryRepository.DiaryRow>();
    for (var d : diaries) {
      diaryByDate.put(d.recordDate(), d);
    }

    var fromInclusive = start.atStartOfDay(CN).toInstant();
    var toExclusive = end.plusDays(1).atStartOfDay(CN).toInstant();
    var assessments =
        assessmentHistoryRepo.listByUserIdAndChildIdBetween(userId, childId, fromInclusive, toExclusive);

    Map<LocalDate, DailyAssessmentHistoryRepository.RecordRow> assessmentByDate = new HashMap<>();
    for (var r : assessments) {
      if (r.submittedAt() == null) continue;
      var day = r.submittedAt().atZone(CN).toLocalDate();
      // Keep the last (latest time) for same day.
      assessmentByDate.put(day, r);
    }

    List<MonthlyAwarenessResponse.MonthlyAwarenessDayView> days = new ArrayList<>();
    for (LocalDate d = start; !d.isAfter(end); d = d.plusDays(1)) {
      var statusCode = statusByDate.get(d);
      var moodId =
          ParentingStatus.fromValue(statusCode).map(ParentingStatus::moodId).orElse(null);

      var scenes =
          troubleScenesByDate.getOrDefault(d, List.of()).stream()
              .map(
                  s ->
                      new MonthlyAwarenessResponse.MonthlyAwarenessTroubleSceneView(
                          s.id(), s.name(), s.logoUrl()))
              .toList();

      MonthlyAwarenessResponse.MonthlyAwarenessAssessmentView assessment = null;
      var a = assessmentByDate.get(d);
      if (a != null && a.submittedAt() != null) {
        assessment =
            new MonthlyAwarenessResponse.MonthlyAwarenessAssessmentView(
                a.assessmentId(),
                ISO_OFFSET.format(a.submittedAt().atZone(CN).toOffsetDateTime()),
                a.aiSummary());
      }

      MonthlyAwarenessResponse.MonthlyAwarenessDiaryView diary = null;
      var dr = diaryByDate.get(d);
      if (dr != null) {
        var content = dr.content() == null ? "" : dr.content();
        var imageUrl = dr.imageUrl() == null ? "" : dr.imageUrl();
        if (!content.isBlank() || !imageUrl.isBlank()) {
          diary = new MonthlyAwarenessResponse.MonthlyAwarenessDiaryView(content, dr.imageUrl());
        }
      }

      days.add(
          new MonthlyAwarenessResponse.MonthlyAwarenessDayView(
              d, statusCode, moodId, scenes, assessment, diary));
    }

    return new MonthlyAwarenessResponse(childId, month.toString(), days);
  }
}
