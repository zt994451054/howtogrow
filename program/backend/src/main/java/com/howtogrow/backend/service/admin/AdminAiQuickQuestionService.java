package com.howtogrow.backend.service.admin;

import com.howtogrow.backend.api.ErrorCode;
import com.howtogrow.backend.api.exception.AppException;
import com.howtogrow.backend.controller.admin.dto.AiQuickQuestionUpsertRequest;
import com.howtogrow.backend.controller.admin.dto.AiQuickQuestionView;
import com.howtogrow.backend.controller.admin.dto.PageResponse;
import com.howtogrow.backend.infrastructure.admin.AiQuickQuestionAdminRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AdminAiQuickQuestionService {
  private final AiQuickQuestionAdminRepository repo;

  public AdminAiQuickQuestionService(AiQuickQuestionAdminRepository repo) {
    this.repo = repo;
  }

  public PageResponse<AiQuickQuestionView> list(int page, int pageSize, Integer status, String keyword) {
    var safeKeyword = safeText(keyword);
    int offset = (page - 1) * pageSize;
    long total = repo.count(safeKeyword, status);
    var items =
        repo.listPage(offset, pageSize, safeKeyword, status).stream()
            .map(
                r ->
                    new AiQuickQuestionView(
                        r.id(), r.prompt(), r.status(), r.sortNo(), r.createdAt(), r.updatedAt()))
            .toList();
    return new PageResponse<>(page, pageSize, total, items);
  }

  @Transactional
  public long create(AiQuickQuestionUpsertRequest request) {
    validate(request);
    return repo.create(request.prompt().trim(), request.status(), request.sortNo());
  }

  @Transactional
  public void update(long id, AiQuickQuestionUpsertRequest request) {
    validate(request);
    repo.findById(id).orElseThrow(() -> new AppException(ErrorCode.NOT_FOUND, "快捷问题不存在"));
    repo.update(id, request.prompt().trim(), request.status(), request.sortNo());
  }

  @Transactional
  public void delete(long id) {
    repo.softDelete(id);
  }

  private static void validate(AiQuickQuestionUpsertRequest request) {
    if (request.prompt() == null || request.prompt().trim().isBlank()) {
      throw new AppException(ErrorCode.INVALID_REQUEST, "prompt 不能为空");
    }
    if (request.status() == null || (request.status() != 0 && request.status() != 1)) {
      throw new AppException(ErrorCode.INVALID_REQUEST, "status 不合法");
    }
    if (request.sortNo() == null) {
      throw new AppException(ErrorCode.INVALID_REQUEST, "sortNo 不能为空");
    }
  }

  private static String safeText(String text) {
    if (text == null) return null;
    var t = text.trim();
    return t.isBlank() ? null : t;
  }
}

