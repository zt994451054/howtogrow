package com.howtogrow.backend.controller.admin;

import com.howtogrow.backend.api.ApiResponse;
import com.howtogrow.backend.api.TraceId;
import com.howtogrow.backend.controller.admin.dto.PageResponse;
import com.howtogrow.backend.controller.admin.dto.TroubleSceneUpsertRequest;
import com.howtogrow.backend.controller.admin.dto.TroubleSceneView;
import com.howtogrow.backend.service.admin.AdminTroubleSceneService;
import io.swagger.v3.oas.annotations.Parameter;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/admin/trouble-scenes")
public class AdminTroubleSceneController {
  private final AdminTroubleSceneService sceneService;

  public AdminTroubleSceneController(AdminTroubleSceneService sceneService) {
    this.sceneService = sceneService;
  }

  @GetMapping
  public ApiResponse<PageResponse<TroubleSceneView>> list(
      @Parameter(description = "页码（从1开始）") @RequestParam(defaultValue = "1") @Min(1) int page,
      @Parameter(description = "每页条数（1-200）") @RequestParam(defaultValue = "20") @Min(1) @Max(200) int pageSize,
      @Parameter(description = "关键字（name 模糊匹配）") @RequestParam(required = false) String keyword,
      @Parameter(description = "按年龄筛选（minAge<=ageYear<=maxAge）") @RequestParam(required = false) @Min(0) @Max(18) Integer ageYear) {
    return ApiResponse.ok(sceneService.list(page, pageSize, keyword, ageYear), TraceId.current());
  }

  @PostMapping
  public ApiResponse<Long> create(@Valid @RequestBody TroubleSceneUpsertRequest request) {
    return ApiResponse.ok(sceneService.create(request), TraceId.current());
  }

  @PutMapping("/{id}")
  public ApiResponse<Void> update(
      @Parameter(description = "场景ID") @PathVariable long id, @Valid @RequestBody TroubleSceneUpsertRequest request) {
    sceneService.update(id, request);
    return ApiResponse.ok(null, TraceId.current());
  }

  @DeleteMapping("/{id}")
  public ApiResponse<Void> delete(@Parameter(description = "场景ID") @PathVariable long id) {
    sceneService.delete(id);
    return ApiResponse.ok(null, TraceId.current());
  }
}
