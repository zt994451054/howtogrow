package com.howtogrow.backend.auth;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
public class SecurityConfig {
  @Bean
  public SecurityFilterChain securityFilterChain(
      HttpSecurity http,
      JwtAuthFilter jwtAuthFilter,
      ApiSecurityExceptionHandlers apiSecurityExceptionHandlers)
      throws Exception {
    return http
        .cors(cors -> {})
        .csrf(csrf -> csrf.disable())
        .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
        .exceptionHandling(
            eh ->
                eh.authenticationEntryPoint(apiSecurityExceptionHandlers)
                    .accessDeniedHandler(apiSecurityExceptionHandlers))
        .authorizeHttpRequests(
            auth ->
                auth.requestMatchers("/actuator/health")
                    .permitAll()
                    .requestMatchers("/error")
                    .permitAll()
                    .requestMatchers("/v3/api-docs/**")
                    .permitAll()
                    .requestMatchers("/swagger-ui/**", "/swagger-ui.html")
                    .permitAll()
                    .requestMatchers(HttpMethod.OPTIONS, "/**")
                    .permitAll()
                    .requestMatchers("/api/v1/miniprogram/auth/**")
                    .permitAll()
                    .requestMatchers("/api/v1/admin/auth/**")
                    .permitAll()
                    .requestMatchers("/api/v1/pay/**")
                    .permitAll()
                    .requestMatchers("/api/v1/miniprogram/quotes/**")
                    .permitAll()
                    .anyRequest()
                    .authenticated())
        .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class)
        .build();
  }
}
