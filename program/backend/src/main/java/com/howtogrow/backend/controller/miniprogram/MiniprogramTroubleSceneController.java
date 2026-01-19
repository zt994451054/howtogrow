package com.howtogrow.backend.controller.miniprogram;

import com.howtogrow.backend.api.ApiResponse;
import com.howtogrow.backend.api.TraceId;
import com.howtogrow.backend.controller.miniprogram.dto.TroubleSceneView;
import com.howtogrow.backend.service.miniprogram.MiniprogramTroubleSceneService;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/miniprogram/trouble-scenes")
public class MiniprogramTroubleSceneController {
  private final MiniprogramTroubleSceneService sceneService;

  public MiniprogramTroubleSceneController(MiniprogramTroubleSceneService sceneService) {
    this.sceneService = sceneService;
  }

  @GetMapping
  public ApiResponse<List<TroubleSceneView>> list() {
    return ApiResponse.ok(sceneService.list(), TraceId.current());
  }
}

