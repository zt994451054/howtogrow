package com.howtogrow.backend.service.admin;

import com.howtogrow.backend.controller.admin.dto.AdminChildView;
import com.howtogrow.backend.controller.admin.dto.PageResponse;
import com.howtogrow.backend.infrastructure.admin.ChildQueryRepository;
import com.howtogrow.backend.infrastructure.admin.ChildQueryRepository.ChildQuery;
import org.springframework.stereotype.Service;

@Service
public class AdminChildService {
  private static final int MAX_PAGE_SIZE = 200;

  private final ChildQueryRepository queryRepo;

  public AdminChildService(ChildQueryRepository queryRepo) {
    this.queryRepo = queryRepo;
  }

  public PageResponse<AdminChildView> list(
      int page,
      int pageSize,
      Long userId,
      String userNickname,
      Long childId,
      String childNickname,
      Integer gender,
      Integer status) {
    int safePageSize = Math.max(1, Math.min(MAX_PAGE_SIZE, pageSize));
    int offset = (page - 1) * safePageSize;

    var query = new ChildQuery(userId, userNickname, childId, childNickname, gender, status);
    long total = queryRepo.countChildren(query);
    var items =
        queryRepo.listChildren(query, offset, safePageSize).stream()
            .map(
                c ->
                    new AdminChildView(
                        c.childId(),
                        c.userId(),
                        c.userNickname(),
                        c.userAvatarUrl(),
                        c.childNickname(),
                        c.gender(),
                        c.birthDate(),
                        c.status(),
                        c.createdAt()))
            .toList();
    return new PageResponse<>(page, safePageSize, total, items);
  }
}

