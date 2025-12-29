package com.howtogrow.backend.controller.admin;

import com.howtogrow.backend.api.ApiResponse;
import com.howtogrow.backend.api.TraceId;
import com.howtogrow.backend.auth.AuthContext;
import com.howtogrow.backend.controller.admin.dto.QuoteCreateRequest;
import com.howtogrow.backend.controller.admin.dto.QuoteUpdateRequest;
import com.howtogrow.backend.controller.admin.dto.QuoteView;
import com.howtogrow.backend.service.admin.AdminQuoteService;
import io.swagger.v3.oas.annotations.Parameter;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/admin/quotes")
@com.howtogrow.backend.auth.AdminPermissionRequired({"QUOTE:MANAGE"})
public class AdminQuoteController {
  private final AdminQuoteService quoteService;

  public AdminQuoteController(AdminQuoteService quoteService) {
    this.quoteService = quoteService;
  }

  @GetMapping
  public ApiResponse<List<QuoteView>> list() {
    return ApiResponse.ok(quoteService.list(), TraceId.current());
  }

  @PostMapping
  public ApiResponse<Void> create(@Valid @RequestBody QuoteCreateRequest request) {
    quoteService.create(request);
    return ApiResponse.ok(null, TraceId.current());
  }

  @PutMapping("/{id}")
  public ApiResponse<Void> update(
      @Parameter(description = "鸡汤语ID") @PathVariable long id,
      @Valid @RequestBody QuoteUpdateRequest request) {
    quoteService.update(id, request);
    return ApiResponse.ok(null, TraceId.current());
  }

  @DeleteMapping("/{id}")
  public ApiResponse<Void> delete(@Parameter(description = "鸡汤语ID") @PathVariable long id) {
    quoteService.delete(id);
    return ApiResponse.ok(null, TraceId.current());
  }
}
