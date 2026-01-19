package com.howtogrow.backend.service.admin;

import com.howtogrow.backend.api.ErrorCode;
import com.howtogrow.backend.api.exception.AppException;
import com.howtogrow.backend.infrastructure.oss.OssProperties;
import com.howtogrow.backend.infrastructure.oss.OssPublicUploader;
import java.io.IOException;
import java.time.Clock;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

@Service
public class AdminUploadService {
  private static final long MAX_IMAGE_BYTES = 5L * 1024 * 1024;
  private static final long MAX_AUDIO_BYTES = 20L * 1024 * 1024;
  private static final long MAX_VIDEO_BYTES = 50L * 1024 * 1024;
  private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("yyyyMMdd");

  private static final Set<String> IMAGE_TYPES =
      Set.of("image/png", "image/jpeg", "image/webp", "image/gif");
  private static final Set<String> AUDIO_TYPES =
      Set.of(
          "audio/mpeg",
          "audio/mp3",
          "audio/mp4",
          "audio/x-m4a",
          "audio/aac",
          "audio/wav",
          "audio/x-wav",
          "audio/ogg");
  private static final Set<String> VIDEO_TYPES =
      Set.of("video/mp4", "video/x-m4v", "video/quicktime", "video/webm");

  private final OssProperties ossProps;
  private final OssPublicUploader uploader;
  private final Clock clock;

  public AdminUploadService(OssProperties ossProps, OssPublicUploader uploader, Clock clock) {
    this.ossProps = ossProps;
    this.uploader = uploader;
    this.clock = clock;
  }

  public String uploadPublic(MultipartFile file) {
    if (file == null || file.isEmpty()) {
      throw new AppException(ErrorCode.INVALID_REQUEST, "请上传文件");
    }

    var contentType = normalizeContentType(file.getContentType());
    var size = file.getSize();
    requireAllowed(contentType, size);

    var extension = inferExtension(contentType, file.getOriginalFilename());
    var today = LocalDate.now(clock).format(DATE);
    var key = "uploads/" + today + "/" + UUID.randomUUID() + "." + extension;

    try (var in = file.getInputStream()) {
      return uploader.uploadPublicObject(key, in, size, contentType);
    } catch (IOException e) {
      throw new AppException(ErrorCode.INTERNAL_ERROR, "读取文件失败");
    }
  }

  private static void requireAllowed(String contentType, long size) {
    if (contentType == null || contentType.isBlank()) {
      throw new AppException(ErrorCode.INVALID_REQUEST, "缺少文件类型");
    }
    if (IMAGE_TYPES.contains(contentType)) {
      if (size > MAX_IMAGE_BYTES) throw new AppException(ErrorCode.INVALID_REQUEST, "图片文件过大");
      return;
    }
    if (AUDIO_TYPES.contains(contentType)) {
      if (size > MAX_AUDIO_BYTES) throw new AppException(ErrorCode.INVALID_REQUEST, "音频文件过大");
      return;
    }
    if (VIDEO_TYPES.contains(contentType)) {
      if (size > MAX_VIDEO_BYTES) throw new AppException(ErrorCode.INVALID_REQUEST, "视频文件过大");
      return;
    }
    throw new AppException(ErrorCode.INVALID_REQUEST, "不支持的文件类型");
  }

  private static String normalizeContentType(String contentType) {
    if (contentType == null) return null;
    return contentType.trim().toLowerCase(Locale.ROOT);
  }

  private static String inferExtension(String contentType, String originalFilename) {
    var ext = extensionFromFilename(originalFilename);
    if (ext != null) return ext;
    return switch (contentType) {
      case "image/png" -> "png";
      case "image/jpeg" -> "jpg";
      case "image/webp" -> "webp";
      case "image/gif" -> "gif";
      case "audio/mpeg", "audio/mp3" -> "mp3";
      case "audio/mp4", "audio/x-m4a" -> "m4a";
      case "audio/aac" -> "aac";
      case "audio/wav", "audio/x-wav" -> "wav";
      case "audio/ogg" -> "ogg";
      case "video/mp4", "video/x-m4v" -> "mp4";
      case "video/quicktime" -> "mov";
      case "video/webm" -> "webm";
      default -> "bin";
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
}

