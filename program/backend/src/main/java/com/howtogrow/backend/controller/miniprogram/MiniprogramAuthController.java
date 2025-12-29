package com.howtogrow.backend.controller.miniprogram;

import com.howtogrow.backend.api.ApiResponse;
import com.howtogrow.backend.api.TraceId;
import com.howtogrow.backend.controller.miniprogram.dto.WechatLoginRequest;
import com.howtogrow.backend.controller.miniprogram.dto.WechatLoginResponse;
import com.howtogrow.backend.service.miniprogram.MiniprogramLoginService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/miniprogram/auth")
public class MiniprogramAuthController {
  private final MiniprogramLoginService loginService;

  public MiniprogramAuthController(MiniprogramLoginService loginService) {
    this.loginService = loginService;
  }

  @PostMapping("/wechat-login")
  public ApiResponse<WechatLoginResponse> wechatLogin(@Valid @RequestBody WechatLoginRequest request) {
    var resp = loginService.login(request.code());
    return ApiResponse.ok(resp, TraceId.current());
  }
}

