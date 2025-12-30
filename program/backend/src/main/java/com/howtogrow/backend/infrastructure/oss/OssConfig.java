package com.howtogrow.backend.infrastructure.oss;

import com.aliyun.oss.OSS;
import com.aliyun.oss.OSSClientBuilder;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConditionalOnProperty(prefix = "app.oss", name = "enabled", havingValue = "true")
public class OssConfig {
  @Bean(destroyMethod = "shutdown")
  public OSS ossClient(OssProperties props) {
    requireNonBlank(props.endpoint(), "app.oss.endpoint");
    requireNonBlank(props.accessKeyId(), "app.oss.access-key-id");
    requireNonBlank(props.accessKeySecret(), "app.oss.access-key-secret");
    requireNonBlank(props.bucket(), "app.oss.bucket");
    return new OSSClientBuilder().build(props.endpoint(), props.accessKeyId(), props.accessKeySecret());
  }

  private static void requireNonBlank(String value, String name) {
    if (value == null || value.isBlank()) {
      throw new IllegalStateException(name + " is required when app.oss.enabled=true");
    }
  }
}

