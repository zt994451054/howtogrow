package com.howtogrow.backend.service.admin;

import com.howtogrow.backend.controller.admin.dto.QuoteCreateRequest;
import com.howtogrow.backend.controller.admin.dto.QuoteUpdateRequest;
import com.howtogrow.backend.controller.admin.dto.QuoteView;
import com.howtogrow.backend.controller.admin.dto.PageResponse;
import com.howtogrow.backend.api.ErrorCode;
import com.howtogrow.backend.api.exception.AppException;
import com.howtogrow.backend.domain.quote.QuoteScene;
import com.howtogrow.backend.infrastructure.admin.QuoteAdminRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AdminQuoteService {
  private final QuoteAdminRepository repo;

  public AdminQuoteService(QuoteAdminRepository repo) {
    this.repo = repo;
  }

  public PageResponse<QuoteView> list(int page, int pageSize, String scene, Integer status, String keyword) {
    var safeScene = safeText(scene);
    var safeKeyword = safeText(keyword);
    if (safeScene != null && QuoteScene.fromValue(safeScene).isEmpty()) {
      throw new AppException(ErrorCode.INVALID_REQUEST, "scene 不合法");
    }
    int offset = (page - 1) * pageSize;
    long total = repo.countQuotes(safeScene, status, safeKeyword);
    var items =
        repo.listQuotes(offset, pageSize, safeScene, status, safeKeyword).stream()
            .map(q -> new QuoteView(q.id(), q.content(), q.scene(), q.minAge(), q.maxAge(), q.status()))
            .toList();
    return new PageResponse<>(page, pageSize, total, items);
  }

  @Transactional
  public void create(QuoteCreateRequest request) {
    validate(request.scene(), request.minAge(), request.maxAge());
    var scene = QuoteScene.fromValue(request.scene()).orElseThrow(() -> new AppException(ErrorCode.INVALID_REQUEST, "场景不合法"));
    repo.create(request.content().trim(), scene.value(), request.minAge(), request.maxAge(), request.status());
  }

  @Transactional
  public void update(long id, QuoteUpdateRequest request) {
    validate(request.scene(), request.minAge(), request.maxAge());
    var scene = QuoteScene.fromValue(request.scene()).orElseThrow(() -> new AppException(ErrorCode.INVALID_REQUEST, "场景不合法"));
    repo.update(id, request.content().trim(), scene.value(), request.minAge(), request.maxAge(), request.status());
  }

  @Transactional
  public void delete(long id) {
    repo.softDelete(id);
  }

  private static void validate(String scene, Integer minAge, Integer maxAge) {
    if (QuoteScene.fromValue(scene).isEmpty()) {
      throw new AppException(ErrorCode.INVALID_REQUEST, "场景不合法");
    }
    if (minAge == null || maxAge == null) {
      throw new AppException(ErrorCode.INVALID_REQUEST, "minAge/maxAge 不能为空");
    }
    if (minAge < 0 || maxAge < 0 || minAge > 18 || maxAge > 18 || minAge > maxAge) {
      throw new AppException(ErrorCode.INVALID_REQUEST, "年龄范围不合法");
    }
  }

  private static String safeText(String text) {
    if (text == null) return null;
    var t = text.trim();
    return t.isBlank() ? null : t;
  }
}
