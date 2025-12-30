package com.howtogrow.backend.config;

import com.howtogrow.backend.auth.JwtProperties;
import com.howtogrow.backend.config.AdminSecurityProperties;
import com.howtogrow.backend.infrastructure.ai.AiProperties;
import com.howtogrow.backend.infrastructure.oss.OssProperties;
import com.howtogrow.backend.infrastructure.pay.WechatPayProperties;
import com.howtogrow.backend.infrastructure.wechat.WechatProperties;
import java.time.Clock;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.task.TaskExecutor;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

@Configuration
@EnableConfigurationProperties({
  JwtProperties.class,
  WechatProperties.class,
  AiProperties.class,
  WechatPayProperties.class,
  RateLimitProperties.class,
  CorsProperties.class,
  AdminSecurityProperties.class,
  DailyAssessmentProperties.class,
  OssProperties.class
})
public class AppConfig {
  @Bean
  public Clock clock() {
    return Clock.systemUTC();
  }

  @Bean
  public TaskExecutor taskExecutor() {
    var executor = new ThreadPoolTaskExecutor();
    executor.setCorePoolSize(4);
    executor.setMaxPoolSize(16);
    executor.setQueueCapacity(200);
    executor.setThreadNamePrefix("app-async-");
    executor.initialize();
    return executor;
  }
}
