package com.howtogrow.backend.service.admin;

import com.howtogrow.backend.controller.admin.dto.AssessmentDimensionScoreView;
import com.howtogrow.backend.controller.admin.dto.AssessmentView;
import com.howtogrow.backend.controller.admin.dto.PageResponse;
import com.howtogrow.backend.domain.capability.CapabilityDimension;
import com.howtogrow.backend.infrastructure.admin.AssessmentQueryRepository;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;

@Service
public class AdminAssessmentService {
  private final AssessmentQueryRepository queryRepo;

  public AdminAssessmentService(AssessmentQueryRepository queryRepo) {
    this.queryRepo = queryRepo;
  }

  public PageResponse<AssessmentView> list(int page, int pageSize) {
    int offset = (page - 1) * pageSize;
    long total = queryRepo.countAssessments();
    var rows = queryRepo.listAssessments(offset, pageSize);

    var ids = rows.stream().map(AssessmentQueryRepository.AssessmentRow::id).toList();
    var scoreRows = queryRepo.listDimensionScoresByAssessmentIds(ids);
    Map<Long, Map<String, Long>> scoreMap = new HashMap<>();
    for (var r : scoreRows) {
      scoreMap.computeIfAbsent(r.assessmentId(), (k) -> new HashMap<>()).put(r.dimensionCode(), r.score());
    }

    var items =
        rows.stream()
            .map(
                a -> {
                  var dimScores = buildDimensionScores(scoreMap.get(a.id()));
                  var dimScoreByCode = toScoreMap(dimScores);
                  return new AssessmentView(
                      a.id(),
                      a.userId(),
                      a.userNickname(),
                      a.userAvatarUrl(),
                      a.childId(),
                      a.childNickname(),
                      a.bizDate(),
                      a.submittedAt(),
                      dimScoreByCode.getOrDefault(CapabilityDimension.EMOTION_MANAGEMENT.code(), 0L),
                      dimScoreByCode.getOrDefault(CapabilityDimension.COMMUNICATION_EXPRESSION.code(), 0L),
                      dimScoreByCode.getOrDefault(CapabilityDimension.RULE_GUIDANCE.code(), 0L),
                      dimScoreByCode.getOrDefault(CapabilityDimension.RELATIONSHIP_BUILDING.code(), 0L),
                      dimScoreByCode.getOrDefault(CapabilityDimension.LEARNING_SUPPORT.code(), 0L),
                      dimScores);
                })
            .toList();
    return new PageResponse<>(page, pageSize, total, items);
  }

  private static List<AssessmentDimensionScoreView> buildDimensionScores(Map<String, Long> byCode) {
    var out = new ArrayList<AssessmentDimensionScoreView>();
    for (var dim : CapabilityDimension.ordered()) {
      long score = 0L;
      if (byCode != null) {
        var v = byCode.get(dim.code());
        if (v != null) score = v;
      }
      out.add(new AssessmentDimensionScoreView(dim.code(), dim.displayName(), score));
    }
    return out;
  }

  private static Map<String, Long> toScoreMap(List<AssessmentDimensionScoreView> dims) {
    Map<String, Long> map = new HashMap<>();
    if (dims == null) return map;
    for (var d : dims) {
      if (d == null || d.dimensionCode() == null) continue;
      map.put(d.dimensionCode(), d.score());
    }
    return map;
  }
}
