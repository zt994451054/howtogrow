package com.howtogrow.backend.service.miniprogram;

import com.howtogrow.backend.api.ErrorCode;
import com.howtogrow.backend.api.exception.AppException;
import com.howtogrow.backend.controller.miniprogram.dto.ChildCreateRequest;
import com.howtogrow.backend.controller.miniprogram.dto.ChildCreateResponse;
import com.howtogrow.backend.controller.miniprogram.dto.ChildUpdateRequest;
import com.howtogrow.backend.controller.miniprogram.dto.ChildView;
import com.howtogrow.backend.domain.child.ParentIdentity;
import com.howtogrow.backend.domain.time.BizClock;
import com.howtogrow.backend.infrastructure.child.ChildRepository;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class MiniprogramChildService {
  private final ChildRepository childRepo;
  private final BizClock bizClock;

  public MiniprogramChildService(ChildRepository childRepo, BizClock bizClock) {
    this.childRepo = childRepo;
    this.bizClock = bizClock;
  }

  public List<ChildView> list(long userId) {
    return childRepo.listByUserId(userId).stream()
        .map(c -> new ChildView(c.id(), c.nickname(), c.gender(), c.birthDate(), c.parentIdentity()))
        .toList();
  }

  @Transactional
  public ChildCreateResponse create(long userId, ChildCreateRequest request) {
    if (request.birthDate().isAfter(bizClock.today())) {
      throw new AppException(ErrorCode.INVALID_REQUEST, "出生日期不能是未来时间");
    }
    var identity =
        ParentIdentity
            .fromValue(request.parentIdentity())
            .orElseThrow(() -> new AppException(ErrorCode.INVALID_REQUEST, "家长身份不合法"));
    var childId =
        childRepo.create(
            userId,
            request.nickname().trim(),
            request.gender(),
            request.birthDate(),
            identity.value());
    return new ChildCreateResponse(childId);
  }

  @Transactional
  public void update(long userId, long childId, ChildUpdateRequest request) {
    if (request.birthDate().isAfter(bizClock.today())) {
      throw new AppException(ErrorCode.INVALID_REQUEST, "出生日期不能是未来时间");
    }
    var identity =
        ParentIdentity
            .fromValue(request.parentIdentity())
            .orElseThrow(() -> new AppException(ErrorCode.INVALID_REQUEST, "家长身份不合法"));
    var existing =
        childRepo.findById(childId).orElseThrow(() -> new AppException(ErrorCode.NOT_FOUND, "孩子不存在"));
    if (existing.userId() != userId) {
      throw new AppException(ErrorCode.FORBIDDEN_RESOURCE, "无权限");
    }
    childRepo.update(
        childId,
        userId,
        request.nickname().trim(),
        request.gender(),
        request.birthDate(),
        identity.value());
  }

  @Transactional
  public void delete(long userId, long childId) {
    var existing =
        childRepo.findById(childId).orElseThrow(() -> new AppException(ErrorCode.NOT_FOUND, "孩子不存在"));
    if (existing.userId() != userId) {
      throw new AppException(ErrorCode.FORBIDDEN_RESOURCE, "无权限");
    }
    childRepo.softDelete(childId, userId);
  }
}
