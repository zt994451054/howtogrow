package com.howtogrow.backend.service.admin;

import com.howtogrow.backend.api.ErrorCode;
import com.howtogrow.backend.api.exception.AppException;
import com.howtogrow.backend.controller.admin.dto.PageResponse;
import com.howtogrow.backend.controller.admin.dto.TroubleSceneUpsertRequest;
import com.howtogrow.backend.controller.admin.dto.TroubleSceneView;
import com.howtogrow.backend.infrastructure.admin.QuestionAdminRepository;
import com.howtogrow.backend.infrastructure.trouble.TroubleSceneRepository;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AdminTroubleSceneService {
  private final TroubleSceneRepository sceneRepo;
  private final QuestionAdminRepository questionRepo;

  public AdminTroubleSceneService(TroubleSceneRepository sceneRepo, QuestionAdminRepository questionRepo) {
    this.sceneRepo = sceneRepo;
    this.questionRepo = questionRepo;
  }

  public PageResponse<TroubleSceneView> list(int page, int pageSize, String keyword, Integer ageYear) {
    var safeKeyword = safeText(keyword);
    int offset = (page - 1) * pageSize;
    long total = sceneRepo.countAdmin(safeKeyword, ageYear);
    var items =
        sceneRepo.listAdminPage(offset, pageSize, safeKeyword, ageYear).stream()
            .map(
                r ->
                    new TroubleSceneView(
                        r.id(), r.name(), r.logoUrl(), r.minAge(), r.maxAge(), r.status(), r.createdAt(), r.updatedAt()))
            .toList();
    return new PageResponse<>(page, pageSize, total, items);
  }

  @Transactional
  public long create(TroubleSceneUpsertRequest request) {
    var name = request.name().trim();
    validateAgeRange(request.minAge(), request.maxAge());
    try {
      return sceneRepo.create(
          name,
          safeText(request.logoUrl()),
          request.minAge(),
          request.maxAge());
    } catch (DuplicateKeyException e) {
      throw new AppException(ErrorCode.INVALID_REQUEST, "名称已存在");
    }
  }

  @Transactional
  public void update(long id, TroubleSceneUpsertRequest request) {
    var existing =
        sceneRepo.findById(id).orElseThrow(() -> new AppException(ErrorCode.NOT_FOUND, "烦恼场景不存在"));
    if (existing.status() == 0) {
      throw new AppException(ErrorCode.INVALID_REQUEST, "烦恼场景已删除");
    }
    validateAgeRange(request.minAge(), request.maxAge());
    try {
      sceneRepo.update(
          id,
          request.name().trim(),
          safeText(request.logoUrl()),
          request.minAge(),
          request.maxAge());
    } catch (DuplicateKeyException e) {
      throw new AppException(ErrorCode.INVALID_REQUEST, "名称已存在");
    }
  }

  @Transactional
  public void delete(long id) {
    var existing =
        sceneRepo.findById(id).orElseThrow(() -> new AppException(ErrorCode.NOT_FOUND, "烦恼场景不存在"));
    if (existing.status() == 0) {
      return;
    }
    questionRepo.deleteQuestionTroubleScenesBySceneId(id);
    sceneRepo.softDelete(id);
  }

  private static String safeText(String text) {
    if (text == null) return null;
    var t = text.trim();
    return t.isBlank() ? null : t;
  }

  private static void validateAgeRange(Integer minAge, Integer maxAge) {
    if (minAge == null || maxAge == null) {
      throw new AppException(ErrorCode.INVALID_REQUEST, "minAge/maxAge 不能为空");
    }
    if (minAge < 0 || maxAge < 0 || minAge > 18 || maxAge > 18 || minAge > maxAge) {
      throw new AppException(ErrorCode.INVALID_REQUEST, "年龄范围不合法");
    }
  }

//  private static String safeText(String text) {
//    if (text == null) return null;
//    var t = text.trim();
//    return t.isBlank() ? null : t;
//  }
}
