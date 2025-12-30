package com.howtogrow.backend.controller.miniprogram;

import com.howtogrow.backend.api.ApiResponse;
import com.howtogrow.backend.api.TraceId;
import com.howtogrow.backend.auth.AuthContext;
import com.howtogrow.backend.controller.miniprogram.dto.UploadAvatarResponse;
import com.howtogrow.backend.service.miniprogram.MiniprogramUploadService;
import io.swagger.v3.oas.annotations.Parameter;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/v1/miniprogram/uploads")
public class MiniprogramUploadController {
  private final MiniprogramUploadService uploadService;

  public MiniprogramUploadController(MiniprogramUploadService uploadService) {
    this.uploadService = uploadService;
  }

  @PostMapping(value = "/avatar", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
  public ApiResponse<UploadAvatarResponse> uploadAvatar(
      @Parameter(description = "头像文件") @RequestPart("file") MultipartFile file) {
    var user = AuthContext.requireMiniprogram();
    var url = uploadService.uploadAvatar(user.userId(), file);
    return ApiResponse.ok(new UploadAvatarResponse(url), TraceId.current());
  }
}

