package com.howtogrow.backend.service.admin;

import com.howtogrow.backend.api.ErrorCode;
import com.howtogrow.backend.api.exception.AppException;
import com.howtogrow.backend.controller.admin.dto.PageResponse;
import com.howtogrow.backend.controller.admin.dto.BannerUpsertRequest;
import com.howtogrow.backend.controller.admin.dto.BannerView;
import com.howtogrow.backend.infrastructure.banner.BannerRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AdminBannerService {
  private static final int MAX_ACTIVE_BANNERS = 5;

  private final BannerRepository bannerRepo;

  public AdminBannerService(BannerRepository bannerRepo) {
    this.bannerRepo = bannerRepo;
  }

  public PageResponse<BannerView> list(int page, int pageSize, Integer status, String keyword) {
    var safeKeyword = safeText(keyword);
    int offset = (page - 1) * pageSize;
    long total = bannerRepo.countAdmin(safeKeyword, status);
    var items =
        bannerRepo.listAdminPage(offset, pageSize, safeKeyword, status).stream()
            .map(
                b ->
                    new BannerView(
                        b.id(),
                        b.title(),
                        b.imageUrl(),
                        b.htmlContent(),
                        b.status(),
                        b.sortNo(),
                        b.createdAt(),
                        b.updatedAt()))
            .toList();
    return new PageResponse<>(page, pageSize, total, items);
  }

  @Transactional
  public long create(BannerUpsertRequest request) {
    validate(request);
    requireMaxActiveIfTurningOn(null, request.status());
    return bannerRepo.create(
        request.title().trim(),
        request.imageUrl().trim(),
        request.htmlContent().trim(),
        request.status(),
        request.sortNo());
  }

  @Transactional
  public void update(long id, BannerUpsertRequest request) {
    validate(request);
    var existing =
        bannerRepo.findById(id).orElseThrow(() -> new AppException(ErrorCode.NOT_FOUND, "Banner 不存在"));
    requireMaxActiveIfTurningOn(existing.status(), request.status());
    bannerRepo.update(
        id,
        request.title().trim(),
        request.imageUrl().trim(),
        request.htmlContent().trim(),
        request.status(),
        request.sortNo());
  }

  @Transactional
  public void delete(long id) {
    bannerRepo.softDelete(id);
  }

  private void validate(BannerUpsertRequest request) {
    if (request.status() == null || (request.status() != 0 && request.status() != 1)) {
      throw new AppException(ErrorCode.INVALID_REQUEST, "status 不合法");
    }
    if (request.sortNo() == null) {
      throw new AppException(ErrorCode.INVALID_REQUEST, "sortNo 不能为空");
    }
  }

  private void requireMaxActiveIfTurningOn(Integer oldStatus, Integer newStatus) {
    if (newStatus == null || newStatus != 1) {
      return;
    }
    if (oldStatus != null && oldStatus == 1) {
      return;
    }
    var activeIds = bannerRepo.listActiveIdsForUpdate();
    if (activeIds.size() >= MAX_ACTIVE_BANNERS) {
      throw new AppException(ErrorCode.INVALID_REQUEST, "最多只能上架 5 个 Banner");
    }
  }

  private static String safeText(String text) {
    if (text == null) return null;
    var t = text.trim();
    return t.isBlank() ? null : t;
  }
}
