package com.howtogrow.backend.service.miniprogram;

import com.howtogrow.backend.api.ErrorCode;
import com.howtogrow.backend.api.exception.AppException;
import com.howtogrow.backend.controller.miniprogram.dto.BannerDetailView;
import com.howtogrow.backend.controller.miniprogram.dto.BannerListItemView;
import com.howtogrow.backend.infrastructure.banner.BannerRepository;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class MiniprogramBannerService {
  private static final int MAX_ACTIVE_BANNERS = 5;

  private final BannerRepository bannerRepo;

  public MiniprogramBannerService(BannerRepository bannerRepo) {
    this.bannerRepo = bannerRepo;
  }

  public List<BannerListItemView> list() {
    return bannerRepo.listActiveForMiniprogram(MAX_ACTIVE_BANNERS).stream()
        .map(b -> new BannerListItemView(b.id(), b.title(), b.imageUrl(), b.sortNo()))
        .toList();
  }

  public BannerDetailView detail(long id) {
    var row =
        bannerRepo
            .findById(id)
            .orElseThrow(() -> new AppException(ErrorCode.NOT_FOUND, "Banner 不存在"));
    if (row.status() != 1) {
      throw new AppException(ErrorCode.NOT_FOUND, "Banner 不存在");
    }
    return new BannerDetailView(row.id(), row.title(), row.htmlContent());
  }
}

