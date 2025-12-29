package com.howtogrow.backend.service.admin;

import com.howtogrow.backend.controller.admin.dto.QuoteCreateRequest;
import com.howtogrow.backend.controller.admin.dto.QuoteUpdateRequest;
import com.howtogrow.backend.controller.admin.dto.QuoteView;
import com.howtogrow.backend.infrastructure.admin.QuoteAdminRepository;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AdminQuoteService {
  private final QuoteAdminRepository repo;

  public AdminQuoteService(QuoteAdminRepository repo) {
    this.repo = repo;
  }

  public List<QuoteView> list() {
    return repo.listAll().stream().map(q -> new QuoteView(q.id(), q.content(), q.status())).toList();
  }

  @Transactional
  public void create(QuoteCreateRequest request) {
    repo.create(request.content().trim(), request.status());
  }

  @Transactional
  public void update(long id, QuoteUpdateRequest request) {
    repo.update(id, request.content().trim(), request.status());
  }

  @Transactional
  public void delete(long id) {
    repo.softDelete(id);
  }
}

