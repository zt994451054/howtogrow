package com.howtogrow.backend.service.miniprogram;

import com.howtogrow.backend.api.ErrorCode;
import com.howtogrow.backend.api.exception.AppException;
import com.howtogrow.backend.controller.miniprogram.dto.AwarenessPersistenceResponse;
import com.howtogrow.backend.domain.time.BizClock;
import com.howtogrow.backend.infrastructure.child.ChildRepository;
import com.howtogrow.backend.infrastructure.report.AwarenessPersistenceRepository;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import org.springframework.stereotype.Service;

@Service
public class AwarenessPersistenceService {
  private final BizClock bizClock;
  private final ChildRepository childRepo;
  private final AwarenessPersistenceRepository persistenceRepo;

  public AwarenessPersistenceService(
      BizClock bizClock, ChildRepository childRepo, AwarenessPersistenceRepository persistenceRepo) {
    this.bizClock = bizClock;
    this.childRepo = childRepo;
    this.persistenceRepo = persistenceRepo;
  }

  public AwarenessPersistenceResponse get(long userId, long childId) {
    var child =
        childRepo.findById(childId).orElseThrow(() -> new AppException(ErrorCode.NOT_FOUND, "孩子不存在"));
    if (child.userId() != userId) {
      throw new AppException(ErrorCode.FORBIDDEN_RESOURCE, "无权限");
    }

    LocalDate today = bizClock.today();
    LocalDate first = persistenceRepo.findFirstRecordDate(userId, childId).orElse(today);

    long days = ChronoUnit.DAYS.between(first, today) + 1;
    if (days < 1) days = 1;

    return new AwarenessPersistenceResponse(childId, first, today, days);
  }
}

