package com.howtogrow.backend.service.admin;

import com.howtogrow.backend.api.ErrorCode;
import com.howtogrow.backend.api.exception.AppException;
import com.howtogrow.backend.controller.admin.dto.PageResponse;
import com.howtogrow.backend.controller.admin.dto.QuestionDetailView;
import com.howtogrow.backend.controller.admin.dto.QuestionSummaryView;
import com.howtogrow.backend.domain.capability.CapabilityDimension;
import com.howtogrow.backend.infrastructure.admin.QuestionQueryRepository;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;

@Service
public class AdminQuestionService {
  private final QuestionQueryRepository queryRepo;

  public AdminQuestionService(QuestionQueryRepository queryRepo) {
    this.queryRepo = queryRepo;
  }

  public PageResponse<QuestionSummaryView> list(Integer ageYear, int page, int pageSize) {
    int offset = (page - 1) * pageSize;
    long total = queryRepo.countQuestions(ageYear);
    var items =
        queryRepo.listQuestions(ageYear, offset, pageSize).stream()
            .map(r -> new QuestionSummaryView(r.id(), r.minAge(), r.maxAge(), r.questionType(), r.status(), r.content()))
            .toList();
    return new PageResponse<>(page, pageSize, total, items);
  }

  public QuestionDetailView detail(long questionId) {
    var rows = queryRepo.getQuestionDetail(questionId);
    if (rows.isEmpty()) {
      throw new AppException(ErrorCode.NOT_FOUND, "question not found");
    }
    var first = rows.get(0);
    Map<Long, QuestionDetailView.OptionView> options = new LinkedHashMap<>();
    Map<Long, List<QuestionDetailView.DimensionScore>> dsByOption = new LinkedHashMap<>();
    for (var row : rows) {
      dsByOption.computeIfAbsent(row.optionId(), k -> new ArrayList<>());
      if (row.dimensionCode() != null && row.dimensionScore() != null) {
        dsByOption
            .get(row.optionId())
            .add(
                new QuestionDetailView.DimensionScore(
                    row.dimensionCode(),
                    CapabilityDimension.displayNameOf(row.dimensionCode()),
                    row.dimensionScore()));
      }
      options.putIfAbsent(
          row.optionId(),
          new QuestionDetailView.OptionView(
              row.optionId(),
              row.optionContent(),
              row.suggestFlag(),
              row.improvementTip(),
              row.optionSortNo(),
              List.of()));
    }
    var optionViews =
        options.values().stream()
            .map(
                o ->
                    new QuestionDetailView.OptionView(
                        o.optionId(),
                        o.content(),
                        o.suggestFlag(),
                        o.improvementTip(),
                        o.sortNo(),
                        dsByOption.getOrDefault(o.optionId(), List.of()).stream()
                            .sorted(Comparator.comparingInt(ds -> CapabilityDimension.sortNoOf(ds.dimensionCode())))
                            .toList()))
            .toList();
    return new QuestionDetailView(
        first.questionId(), first.minAge(), first.maxAge(), first.questionType(), first.questionContent(), optionViews);
  }
}
