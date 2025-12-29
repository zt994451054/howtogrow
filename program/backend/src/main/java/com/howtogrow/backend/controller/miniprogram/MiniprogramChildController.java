package com.howtogrow.backend.controller.miniprogram;

import com.howtogrow.backend.api.ApiResponse;
import com.howtogrow.backend.api.TraceId;
import com.howtogrow.backend.auth.AuthContext;
import com.howtogrow.backend.controller.miniprogram.dto.ChildCreateRequest;
import com.howtogrow.backend.controller.miniprogram.dto.ChildCreateResponse;
import com.howtogrow.backend.controller.miniprogram.dto.ChildUpdateRequest;
import com.howtogrow.backend.controller.miniprogram.dto.ChildView;
import com.howtogrow.backend.service.miniprogram.MiniprogramChildService;
import jakarta.validation.Valid;
import java.util.List;
import io.swagger.v3.oas.annotations.Parameter;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/miniprogram/children")
public class MiniprogramChildController {
  private final MiniprogramChildService childService;

  public MiniprogramChildController(MiniprogramChildService childService) {
    this.childService = childService;
  }

  @GetMapping
  public ApiResponse<List<ChildView>> list() {
    var user = AuthContext.requireMiniprogram();
    return ApiResponse.ok(childService.list(user.userId()), TraceId.current());
  }

  @PostMapping
  public ApiResponse<ChildCreateResponse> create(@Valid @RequestBody ChildCreateRequest request) {
    var user = AuthContext.requireMiniprogram();
    return ApiResponse.ok(childService.create(user.userId(), request), TraceId.current());
  }

  @PutMapping("/{childId}")
  public ApiResponse<Void> update(
      @Parameter(description = "孩子ID") @PathVariable long childId,
      @Valid @RequestBody ChildUpdateRequest request) {
    var user = AuthContext.requireMiniprogram();
    childService.update(user.userId(), childId, request);
    return ApiResponse.ok(null, TraceId.current());
  }

  @DeleteMapping("/{childId}")
  public ApiResponse<Void> delete(@Parameter(description = "孩子ID") @PathVariable long childId) {
    var user = AuthContext.requireMiniprogram();
    childService.delete(user.userId(), childId);
    return ApiResponse.ok(null, TraceId.current());
  }
}
