package com.howtogrow.backend.service.miniprogram;

import com.howtogrow.backend.controller.miniprogram.dto.QuoteResponse;
import com.howtogrow.backend.infrastructure.quote.QuoteRepository;
import org.springframework.stereotype.Service;

@Service
public class MiniprogramQuoteService {
  private final QuoteRepository quoteRepo;

  public MiniprogramQuoteService(QuoteRepository quoteRepo) {
    this.quoteRepo = quoteRepo;
  }

  public QuoteResponse random() {
    var content = quoteRepo.pickRandomActive().orElse("今天也辛苦了，先照顾好自己。");
    return new QuoteResponse(content);
  }
}

