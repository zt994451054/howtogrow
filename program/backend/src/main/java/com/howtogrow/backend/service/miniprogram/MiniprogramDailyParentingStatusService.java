package com.howtogrow.backend.service.miniprogram;

import com.howtogrow.backend.api.ErrorCode;
import com.howtogrow.backend.api.exception.AppException;
import com.howtogrow.backend.controller.miniprogram.dto.DailyParentingStatusUpsertRequest;
import com.howtogrow.backend.controller.miniprogram.dto.DailyParentingStatusView;
import com.howtogrow.backend.domain.parenting.ParentingStatus;
import com.howtogrow.backend.domain.time.BizClock;
import com.howtogrow.backend.infrastructure.child.ChildRepository;
import com.howtogrow.backend.infrastructure.parenting.DailyParentingStatusRepository;
import java.time.LocalDate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class MiniprogramDailyParentingStatusService {
  private final BizClock bizClock;
  private final ChildRepository childRepo;
  private final DailyParentingStatusRepository statusRepo;

  public MiniprogramDailyParentingStatusService(
      BizClock bizClock, ChildRepository childRepo, DailyParentingStatusRepository statusRepo) {
    this.bizClock = bizClock;
    this.childRepo = childRepo;
    this.statusRepo = statusRepo;
  }

  public DailyParentingStatusView get(long userId, long childId, LocalDate recordDate) {
    var child =
        childRepo.findById(childId).orElseThrow(() -> new AppException(ErrorCode.NOT_FOUND, "孩子不存在"));
    if (child.userId() != userId) {
      throw new AppException(ErrorCode.FORBIDDEN_RESOURCE, "无权限");
    }
    var day = recordDate == null ? bizClock.today() : recordDate;
    var status = statusRepo.findStatusCode(userId, childId, day).orElse(null);
    if (status == null || status.isBlank()) {
      return null;
    }
    return new DailyParentingStatusView(childId, day, status);
  }

  @Transactional
  public void upsert(long userId, DailyParentingStatusUpsertRequest request) {
    var childId = requireChildId(request.childId());
    var child =
        childRepo.findById(childId).orElseThrow(() -> new AppException(ErrorCode.NOT_FOUND, "孩子不存在"));
    if (child.userId() != userId) {
      throw new AppException(ErrorCode.FORBIDDEN_RESOURCE, "无权限");
    }

    var day = request.recordDate() == null ? bizClock.today() : request.recordDate();
    if (day.isAfter(bizClock.today())) {
      throw new AppException(ErrorCode.INVALID_REQUEST, "记录日期不能是未来时间");
    }

    var normalized =
        ParentingStatus
            .fromValue(request.statusCode())
            .orElseThrow(() -> new AppException(ErrorCode.INVALID_REQUEST, "育儿状态不合法"));
    statusRepo.upsert(userId, childId, day, normalized.value());
  }

  private static long requireChildId(Long childId) {
    if (childId == null || childId <= 0) {
      throw new AppException(ErrorCode.INVALID_REQUEST, "childId 不合法");
    }
    return childId;
  }
}

