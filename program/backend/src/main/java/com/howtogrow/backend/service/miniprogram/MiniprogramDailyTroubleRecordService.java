package com.howtogrow.backend.service.miniprogram;

import com.howtogrow.backend.api.ErrorCode;
import com.howtogrow.backend.api.exception.AppException;
import com.howtogrow.backend.controller.miniprogram.dto.DailyTroubleRecordUpsertRequest;
import com.howtogrow.backend.controller.miniprogram.dto.DailyTroubleRecordView;
import com.howtogrow.backend.domain.time.BizClock;
import com.howtogrow.backend.infrastructure.child.ChildRepository;
import com.howtogrow.backend.infrastructure.trouble.DailyTroubleRecordRepository;
import com.howtogrow.backend.infrastructure.trouble.TroubleSceneRepository;
import java.time.LocalDate;
import java.util.HashSet;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class MiniprogramDailyTroubleRecordService {
  private final BizClock bizClock;
  private final ChildRepository childRepo;
  private final TroubleSceneRepository sceneRepo;
  private final DailyTroubleRecordRepository recordRepo;

  public MiniprogramDailyTroubleRecordService(
      BizClock bizClock,
      ChildRepository childRepo,
      TroubleSceneRepository sceneRepo,
      DailyTroubleRecordRepository recordRepo) {
    this.bizClock = bizClock;
    this.childRepo = childRepo;
    this.sceneRepo = sceneRepo;
    this.recordRepo = recordRepo;
  }

  public DailyTroubleRecordView get(long userId, long childId, LocalDate recordDate) {
    var child =
        childRepo.findById(childId).orElseThrow(() -> new AppException(ErrorCode.NOT_FOUND, "孩子不存在"));
    if (child.userId() != userId) {
      throw new AppException(ErrorCode.FORBIDDEN_RESOURCE, "无权限");
    }
    var day = recordDate == null ? bizClock.today() : recordDate;
    var scenes = recordRepo.listActiveScenes(userId, childId, day);
    if (scenes.isEmpty()) {
      return null;
    }
    return new DailyTroubleRecordView(
        childId,
        day,
        scenes.stream()
            .map(s -> new DailyTroubleRecordView.TroubleSceneView(s.id(), s.name(), s.logoUrl()))
            .toList());
  }

  @Transactional
  public void upsert(long userId, DailyTroubleRecordUpsertRequest request) {
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

    var sceneIds = request.sceneIds();
    if (sceneIds == null || sceneIds.isEmpty()) {
      throw new AppException(ErrorCode.INVALID_REQUEST, "请至少选择 1 个烦恼场景");
    }
    var unique = new HashSet<Long>();
    for (var id : sceneIds) {
      if (id != null && id > 0) unique.add(id);
    }
    if (unique.isEmpty()) {
      throw new AppException(ErrorCode.INVALID_REQUEST, "请至少选择 1 个烦恼场景");
    }

    var existing = sceneRepo.listActiveIds(List.copyOf(unique));
    if (existing.size() != unique.size()) {
      throw new AppException(ErrorCode.INVALID_REQUEST, "烦恼场景不存在或已删除");
    }

    var recordId = recordRepo.upsertRecord(userId, childId, day);
    recordRepo.replaceScenes(recordId, List.copyOf(unique));
  }

  private static long requireChildId(Long childId) {
    if (childId == null || childId <= 0) {
      throw new AppException(ErrorCode.INVALID_REQUEST, "childId 不合法");
    }
    return childId;
  }
}

