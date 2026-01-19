package com.howtogrow.backend.controller.admin;

import com.howtogrow.backend.api.ApiResponse;
import com.howtogrow.backend.api.TraceId;
import com.howtogrow.backend.controller.admin.dto.UploadPublicResponse;
import com.howtogrow.backend.service.admin.AdminUploadService;
import io.swagger.v3.oas.annotations.Parameter;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/v1/admin/uploads")
public class AdminUploadController {
  private final AdminUploadService uploadService;

  public AdminUploadController(AdminUploadService uploadService) {
    this.uploadService = uploadService;
  }

  @PostMapping(value = "/public", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
  public ApiResponse<UploadPublicResponse> uploadPublic(
      @Parameter(description = "文件（图片/音频/视频）") @RequestPart("file") MultipartFile file) {
    var url = uploadService.uploadPublic(file);
    return ApiResponse.ok(new UploadPublicResponse(url), TraceId.current());
  }
}

