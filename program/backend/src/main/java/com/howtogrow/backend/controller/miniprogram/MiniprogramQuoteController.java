package com.howtogrow.backend.controller.miniprogram;

import com.howtogrow.backend.api.ApiResponse;
import com.howtogrow.backend.api.TraceId;
import com.howtogrow.backend.auth.AuthContext;
import com.howtogrow.backend.controller.miniprogram.dto.QuoteResponse;
import com.howtogrow.backend.service.miniprogram.MiniprogramQuoteService;
import io.swagger.v3.oas.annotations.Parameter;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/miniprogram/quotes")
public class MiniprogramQuoteController {
  private final MiniprogramQuoteService quoteService;

  public MiniprogramQuoteController(MiniprogramQuoteService quoteService) {
    this.quoteService = quoteService;
  }

  @GetMapping("/random")
  public ApiResponse<List<QuoteResponse>> random(
      @Parameter(description = "孩子ID") @RequestParam @Min(1) long childId,
      @Parameter(description = "场景：每日觉察/育儿状态/烦恼档案/育儿日记") @RequestParam @NotBlank String scene) {
    var user = AuthContext.requireMiniprogram();
    return ApiResponse.ok(quoteService.random(user.userId(), childId, scene), TraceId.current());
  }
}
