package com.howtogrow.backend.service.admin;

import com.howtogrow.backend.controller.admin.dto.AssessmentView;
import com.howtogrow.backend.controller.admin.dto.PageResponse;
import com.howtogrow.backend.infrastructure.admin.AssessmentQueryRepository;
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
    var items =
        queryRepo.listAssessments(offset, pageSize).stream()
            .map(
                a ->
                    new AssessmentView(
                        a.id(),
                        a.userId(),
                        a.userNickname(),
                        a.childId(),
                        a.childNickname(),
                        a.bizDate(),
                        a.submittedAt()))
            .toList();
    return new PageResponse<>(page, pageSize, total, items);
  }
}
