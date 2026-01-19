package com.howtogrow.backend.controller.miniprogram;

import com.howtogrow.backend.api.ApiResponse;
import com.howtogrow.backend.api.TraceId;
import com.howtogrow.backend.controller.miniprogram.dto.BannerDetailView;
import com.howtogrow.backend.controller.miniprogram.dto.BannerListItemView;
import com.howtogrow.backend.service.miniprogram.MiniprogramBannerService;
import io.swagger.v3.oas.annotations.Parameter;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/miniprogram/banners")
public class MiniprogramBannerController {
  private final MiniprogramBannerService bannerService;

  public MiniprogramBannerController(MiniprogramBannerService bannerService) {
    this.bannerService = bannerService;
  }

  @GetMapping
  public ApiResponse<List<BannerListItemView>> list() {
    return ApiResponse.ok(bannerService.list(), TraceId.current());
  }

  @GetMapping("/{id}")
  public ApiResponse<BannerDetailView> detail(@Parameter(description = "Banner ID") @PathVariable long id) {
    return ApiResponse.ok(bannerService.detail(id), TraceId.current());
  }
}

