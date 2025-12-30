package com.howtogrow.backend.infrastructure.oss;

import com.aliyun.oss.OSS;
import com.aliyun.oss.model.CannedAccessControlList;
import com.aliyun.oss.model.ObjectMetadata;
import com.aliyun.oss.model.PutObjectRequest;
import com.howtogrow.backend.api.ErrorCode;
import com.howtogrow.backend.api.exception.AppException;
import java.io.InputStream;
import java.net.URI;
import java.util.Objects;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Service;

@Service
public class OssPublicUploader {
  private final OssProperties props;
  private final ObjectProvider<OSS> ossProvider;

  public OssPublicUploader(OssProperties props, ObjectProvider<OSS> ossProvider) {
    this.props = props;
    this.ossProvider = ossProvider;
  }

  public String uploadPublicObject(
      String objectKey, InputStream input, long contentLength, String contentType) {
    if (!props.enabled()) {
      throw new AppException(ErrorCode.INTERNAL_ERROR, "oss is not configured");
    }

    var oss = ossProvider.getIfAvailable();
    if (oss == null) {
      throw new AppException(ErrorCode.INTERNAL_ERROR, "oss client is not available");
    }

    var metadata = new ObjectMetadata();
    if (contentLength >= 0) {
      metadata.setContentLength(contentLength);
    }
    if (contentType != null && !contentType.isBlank()) {
      metadata.setContentType(contentType);
    }

    oss.putObject(new PutObjectRequest(props.bucket(), objectKey, input, metadata));
    oss.setObjectAcl(props.bucket(), objectKey, CannedAccessControlList.PublicRead);
    return buildPublicUrl(objectKey);
  }

  private String buildPublicUrl(String objectKey) {
    var base = props.publicBaseUrl();
    if (base != null && !base.isBlank()) {
      return trimTrailingSlash(base) + "/" + objectKey;
    }

    var endpoint = props.endpoint();
    if (endpoint == null || endpoint.isBlank()) {
      throw new AppException(ErrorCode.INTERNAL_ERROR, "oss endpoint is missing");
    }

    var endpointUri = toUri(endpoint);
    var host = Objects.requireNonNull(endpointUri.getHost(), "oss endpoint host is missing");
    var scheme = endpointUri.getScheme() == null || endpointUri.getScheme().isBlank() ? "https" : endpointUri.getScheme();
    return scheme + "://" + props.bucket() + "." + host + "/" + objectKey;
  }

  private static URI toUri(String endpoint) {
    var normalized = endpoint.contains("://") ? endpoint : "https://" + endpoint;
    return URI.create(normalized);
  }

  private static String trimTrailingSlash(String value) {
    var s = value;
    while (s.endsWith("/")) {
      s = s.substring(0, s.length() - 1);
    }
    return s;
  }
}

