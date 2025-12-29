package com.howtogrow.backend.controller.miniprogram;

import com.howtogrow.backend.api.ApiResponse;
import com.howtogrow.backend.api.TraceId;
import com.howtogrow.backend.controller.miniprogram.dto.QuoteResponse;
import com.howtogrow.backend.service.miniprogram.MiniprogramQuoteService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/miniprogram/quotes")
public class MiniprogramQuoteController {
  private final MiniprogramQuoteService quoteService;

  public MiniprogramQuoteController(MiniprogramQuoteService quoteService) {
    this.quoteService = quoteService;
  }

  @GetMapping("/random")
  public ApiResponse<QuoteResponse> random() {
    return ApiResponse.ok(quoteService.random(), TraceId.current());
  }
}

