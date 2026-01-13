package com.howtogrow.backend.service.miniprogram;

import com.howtogrow.backend.api.ErrorCode;
import com.howtogrow.backend.api.exception.AppException;
import com.howtogrow.backend.infrastructure.oss.OssProperties;
import com.howtogrow.backend.infrastructure.oss.OssPublicUploader;
import java.io.IOException;
import java.time.Clock;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

@Service
public class MiniprogramUploadService {
  private static final long MAX_AVATAR_BYTES = 5L * 1024 * 1024;
  private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("yyyyMMdd");

  private final OssProperties ossProps;
  private final OssPublicUploader uploader;
  private final Clock clock;

  public MiniprogramUploadService(OssProperties ossProps, OssPublicUploader uploader, Clock clock) {
    this.ossProps = ossProps;
    this.uploader = uploader;
    this.clock = clock;
  }

  public String uploadAvatar(long userId, MultipartFile file) {
    if (file == null || file.isEmpty()) {
      throw new AppException(ErrorCode.INVALID_REQUEST, "请上传文件");
    }
    if (file.getSize() > MAX_AVATAR_BYTES) {
      throw new AppException(ErrorCode.INVALID_REQUEST, "文件过大");
    }

    var contentType = file.getContentType();
    if (contentType == null || !contentType.startsWith("image/")) {
      throw new AppException(ErrorCode.INVALID_REQUEST, "仅支持图片文件");
    }

    var extension = inferExtension(contentType, file.getOriginalFilename());
    var today = LocalDate.now(clock).format(DATE);
    var prefix = ossProps.avatarPrefix() == null || ossProps.avatarPrefix().isBlank() ? "avatars" : trimSlashes(ossProps.avatarPrefix());
    var key =
        prefix
            + "/"
            + userId
            + "/"
            + today
            + "/"
            + UUID.randomUUID()
            + "."
            + extension;

    try (var in = file.getInputStream()) {
      return uploader.uploadPublicObject(key, in, file.getSize(), contentType);
    } catch (IOException e) {
      throw new AppException(ErrorCode.INTERNAL_ERROR, "读取文件失败");
    }
  }

  private static String inferExtension(String contentType, String originalFilename) {
    var ext = extensionFromFilename(originalFilename);
    if (ext != null) return ext;
    return switch (contentType.toLowerCase(Locale.ROOT)) {
      case "image/png" -> "png";
      case "image/jpeg" -> "jpg";
      case "image/webp" -> "webp";
      case "image/gif" -> "gif";
      default -> "png";
    };
  }

  private static String extensionFromFilename(String filename) {
    if (filename == null) return null;
    var idx = filename.lastIndexOf('.');
    if (idx < 0 || idx == filename.length() - 1) return null;
    var ext = filename.substring(idx + 1).toLowerCase(Locale.ROOT);
    if (ext.length() > 8) return null;
    if (!ext.matches("[a-z0-9]+")) return null;
    return ext;
  }

  private static String trimSlashes(String value) {
    var s = value.trim();
    while (s.startsWith("/")) s = s.substring(1);
    while (s.endsWith("/")) s = s.substring(0, s.length() - 1);
    return s;
  }
}
