package com.howtogrow.backend.service.miniprogram;

import com.howtogrow.backend.api.ErrorCode;
import com.howtogrow.backend.api.exception.AppException;
import com.howtogrow.backend.controller.miniprogram.dto.DailyParentingDiaryUpsertRequest;
import com.howtogrow.backend.controller.miniprogram.dto.DailyParentingDiaryView;
import com.howtogrow.backend.domain.time.BizClock;
import com.howtogrow.backend.infrastructure.child.ChildRepository;
import com.howtogrow.backend.infrastructure.parenting.DailyParentingDiaryRepository;
import java.time.LocalDate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class MiniprogramDailyParentingDiaryService {
  private static final int MAX_CONTENT_CHARS = 5000;

  private final BizClock bizClock;
  private final ChildRepository childRepo;
  private final DailyParentingDiaryRepository diaryRepo;

  public MiniprogramDailyParentingDiaryService(
      BizClock bizClock, ChildRepository childRepo, DailyParentingDiaryRepository diaryRepo) {
    this.bizClock = bizClock;
    this.childRepo = childRepo;
    this.diaryRepo = diaryRepo;
  }

  public DailyParentingDiaryView get(long userId, long childId, LocalDate recordDate) {
    var child =
        childRepo.findById(childId).orElseThrow(() -> new AppException(ErrorCode.NOT_FOUND, "孩子不存在"));
    if (child.userId() != userId) {
      throw new AppException(ErrorCode.FORBIDDEN_RESOURCE, "无权限");
    }
    var day = recordDate == null ? bizClock.today() : recordDate;
    var row = diaryRepo.findByUserChildDate(userId, childId, day).orElse(null);
    if (row == null) {
      return null;
    }
    var content = row.content() == null ? "" : row.content();
    var imageUrl = row.imageUrl() == null ? null : row.imageUrl();
    if (content.isBlank() && (imageUrl == null || imageUrl.isBlank())) {
      return null;
    }
    return new DailyParentingDiaryView(childId, day, content, imageUrl);
  }

  @Transactional
  public void upsert(long userId, DailyParentingDiaryUpsertRequest request) {
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

    var content = request.content() == null ? "" : request.content().trim();
    if (content.length() > MAX_CONTENT_CHARS) {
      throw new AppException(ErrorCode.INVALID_REQUEST, "日记内容过长");
    }

    var imageUrl = request.imageUrl() == null ? null : request.imageUrl().trim();
    if (imageUrl != null && imageUrl.isBlank()) {
      imageUrl = null;
    }
    if (content.isBlank() && imageUrl == null) {
      throw new AppException(ErrorCode.INVALID_REQUEST, "请填写日记内容或上传配图");
    }

    diaryRepo.upsert(userId, childId, day, content, imageUrl);
  }

  private static long requireChildId(Long childId) {
    if (childId == null || childId <= 0) {
      throw new AppException(ErrorCode.INVALID_REQUEST, "childId 不合法");
    }
    return childId;
  }
}
