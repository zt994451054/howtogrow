package com.howtogrow.backend.service.miniprogram;

import com.howtogrow.backend.api.ErrorCode;
import com.howtogrow.backend.api.exception.AppException;
import com.howtogrow.backend.controller.miniprogram.dto.DailyAssessmentAnswerView;
import com.howtogrow.backend.controller.miniprogram.dto.DailyAssessmentItemView;
import com.howtogrow.backend.controller.miniprogram.dto.DailyAssessmentRecordDetailResponse;
import com.howtogrow.backend.controller.miniprogram.dto.DailyAssessmentRecordView;
import com.howtogrow.backend.controller.miniprogram.dto.QuestionOptionView;
import com.howtogrow.backend.infrastructure.assessment.DailyAssessmentHistoryRepository;
import com.howtogrow.backend.infrastructure.question.QuestionSnapshotViewRepository;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;

@Service
public class DailyAssessmentHistoryService {
  private static final int MAX_LIMIT = 100;
  private static final DateTimeFormatter ISO = DateTimeFormatter.ISO_OFFSET_DATE_TIME;
  private static final ZoneOffset CN = ZoneOffset.ofHours(8);

  private final DailyAssessmentHistoryRepository historyRepo;
  private final QuestionSnapshotViewRepository questionViewRepo;

  public DailyAssessmentHistoryService(
      DailyAssessmentHistoryRepository historyRepo, QuestionSnapshotViewRepository questionViewRepo) {
    this.historyRepo = historyRepo;
    this.questionViewRepo = questionViewRepo;
  }

  public List<DailyAssessmentRecordView> list(long userId, int limit, int offset) {
    int safeLimit = Math.min(Math.max(limit, 1), MAX_LIMIT);
    int safeOffset = Math.max(offset, 0);

    return historyRepo.listByUserId(userId, safeLimit, safeOffset).stream()
        .map(
            r ->
                new DailyAssessmentRecordView(
                    r.assessmentId(),
                    r.childId(),
                    r.childName() == null || r.childName().isBlank() ? "（未知）" : r.childName(),
                    r.submittedAt() == null ? "" : ISO.format(r.submittedAt().atOffset(CN)),
                    r.aiSummary()))
        .toList();
  }

  public DailyAssessmentRecordDetailResponse detail(long userId, long assessmentId) {
    var record =
        historyRepo
            .findByUserIdAndAssessmentId(userId, assessmentId)
            .orElseThrow(() -> new AppException(ErrorCode.NOT_FOUND, "测评不存在"));

    var items = historyRepo.listItems(assessmentId);
    var questionIds = items.stream().map(DailyAssessmentHistoryRepository.ItemRow::questionId).distinct().toList();
    var questionOptionRows = questionViewRepo.listQuestionOptionRows(questionIds);

    var questionInfo = new HashMap<Long, QuestionInfo>();
    for (var row : questionOptionRows) {
      var info = questionInfo.computeIfAbsent(
          row.questionId(),
          (qid) -> new QuestionInfo(row.questionContent(), row.questionType(), new ArrayList<>()));
      info.options.add(
          new QuestionOptionView(
              row.optionId(),
              row.optionContent(),
              row.optionSortNo(),
              row.suggestFlag(),
              row.improvementTip()));
    }

    List<DailyAssessmentItemView> itemViews = new ArrayList<>();
    for (var it : items) {
      var info = questionInfo.get(it.questionId());
      if (info == null) {
        itemViews.add(new DailyAssessmentItemView(it.displayOrder(), it.questionId(), "（题目已不存在）", "SINGLE", List.of()));
        continue;
      }
      itemViews.add(
          new DailyAssessmentItemView(
              it.displayOrder(),
              it.questionId(),
              info.content(),
              info.questionType() == null || info.questionType().isBlank() ? "SINGLE" : info.questionType(),
              info.options()));
    }

    var answers = historyRepo.listAnswers(assessmentId);
    Map<Long, List<Long>> answerMap =
        answers.stream().collect(Collectors.groupingBy(DailyAssessmentHistoryRepository.AnswerRow::questionId,
            Collectors.mapping(DailyAssessmentHistoryRepository.AnswerRow::optionId, Collectors.toList())));

    var answerViews =
        answerMap.entrySet().stream()
            .map((e) -> new DailyAssessmentAnswerView(e.getKey(), e.getValue()))
            .toList();

    return new DailyAssessmentRecordDetailResponse(
        record.assessmentId(),
        record.childId(),
        record.childName() == null || record.childName().isBlank() ? "（未知）" : record.childName(),
        record.submittedAt() == null ? "" : ISO.format(record.submittedAt().atOffset(CN)),
        record.aiSummary(),
        itemViews,
        answerViews);
  }

  private record QuestionInfo(String content, String questionType, List<QuestionOptionView> options) {}
}
