package com.howtogrow.backend.controller.miniprogram;

import com.howtogrow.backend.api.ApiResponse;
import com.howtogrow.backend.api.TraceId;
import com.howtogrow.backend.auth.AuthContext;
import com.howtogrow.backend.controller.miniprogram.dto.SubscriptionOrderCreateRequest;
import com.howtogrow.backend.controller.miniprogram.dto.SubscriptionOrderCreateResponse;
import com.howtogrow.backend.controller.miniprogram.dto.SubscriptionPlanView;
import com.howtogrow.backend.service.miniprogram.MiniprogramSubscriptionService;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/miniprogram/subscriptions")
public class MiniprogramSubscriptionController {
  private final MiniprogramSubscriptionService subscriptionService;

  public MiniprogramSubscriptionController(MiniprogramSubscriptionService subscriptionService) {
    this.subscriptionService = subscriptionService;
  }

  @GetMapping("/plans")
  public ApiResponse<List<SubscriptionPlanView>> plans() {
    return ApiResponse.ok(subscriptionService.listPlans(), TraceId.current());
  }

  @PostMapping("/orders")
  public ApiResponse<SubscriptionOrderCreateResponse> createOrder(
      @Valid @RequestBody SubscriptionOrderCreateRequest request) {
    var user = AuthContext.requireMiniprogram();
    return ApiResponse.ok(
        subscriptionService.createOrder(user.userId(), request.planId()), TraceId.current());
  }
}

