package com.howtogrow.backend.service.admin;

import com.howtogrow.backend.controller.admin.dto.PageResponse;
import com.howtogrow.backend.controller.admin.dto.UserView;
import com.howtogrow.backend.infrastructure.admin.UserQueryRepository;
import org.springframework.stereotype.Service;

@Service
public class AdminUserService {
  private final UserQueryRepository queryRepo;

  public AdminUserService(UserQueryRepository queryRepo) {
    this.queryRepo = queryRepo;
  }

  public PageResponse<UserView> list(int page, int pageSize) {
    int offset = (page - 1) * pageSize;
    long total = queryRepo.countUsers();
    var items =
        queryRepo.listUsers(offset, pageSize).stream()
            .map(
                u ->
                    new UserView(
                        u.id(),
                        u.wechatOpenid(),
                        u.nickname(),
                        u.avatarUrl(),
                        u.subscriptionEndAt(),
                        u.freeTrialUsed(),
                        u.createdAt()))
            .toList();
    return new PageResponse<>(page, pageSize, total, items);
  }
}
