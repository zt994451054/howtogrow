package com.howtogrow.backend.config;

import com.howtogrow.backend.api.TraceId;
import java.util.List;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

@Configuration
public class CorsConfig {
  @Bean
  public CorsConfigurationSource corsConfigurationSource(CorsProperties corsProperties) {
    var config = new CorsConfiguration();

    var allowedOrigins = corsProperties.allowedOrigins();
    if (allowedOrigins == null || allowedOrigins.isEmpty()) {
      allowedOrigins = List.of("*");
    }
    if (allowedOrigins.size() == 1 && "*".equals(allowedOrigins.get(0))) {
      config.setAllowedOriginPatterns(List.of("*"));
    } else {
      config.setAllowedOrigins(allowedOrigins);
    }

    config.setAllowCredentials(corsProperties.allowCredentials());
    config.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS"));
    config.setAllowedHeaders(List.of("*"));
    config.setExposedHeaders(List.of(TraceId.HEADER));
    config.setMaxAge(3600L);

    var source = new UrlBasedCorsConfigurationSource();
    source.registerCorsConfiguration("/**", config);
    return source;
  }
}

