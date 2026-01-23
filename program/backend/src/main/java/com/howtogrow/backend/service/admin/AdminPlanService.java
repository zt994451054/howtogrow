package com.howtogrow.backend.service.admin;

import com.howtogrow.backend.api.ErrorCode;
import com.howtogrow.backend.api.exception.AppException;
import com.howtogrow.backend.controller.admin.dto.PlanCreateRequest;
import com.howtogrow.backend.controller.admin.dto.PlanUpdateRequest;
import com.howtogrow.backend.controller.admin.dto.PlanView;
import com.howtogrow.backend.infrastructure.admin.PlanAdminRepository;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AdminPlanService {
  private final PlanAdminRepository repo;

  public AdminPlanService(PlanAdminRepository repo) {
    this.repo = repo;
  }

  public List<PlanView> list() {
    return repo.listAll().stream()
        .map(p -> new PlanView(p.id(), p.name(), p.days(), p.originalPriceCent(), p.priceCent(), p.status()))
        .toList();
  }

  @Transactional
  public void create(PlanCreateRequest request) {
    if (request.days() <= 0
        || request.originalPriceCent() < 0
        || request.priceCent() < 0
        || request.originalPriceCent() < request.priceCent()) {
      throw new AppException(ErrorCode.INVALID_REQUEST, "套餐参数不合法");
    }
    repo.create(
        request.name().trim(),
        request.days(),
        request.originalPriceCent(),
        request.priceCent(),
        request.status());
  }

  @Transactional
  public void update(long id, PlanUpdateRequest request) {
    if (request.days() <= 0
        || request.originalPriceCent() < 0
        || request.priceCent() < 0
        || request.originalPriceCent() < request.priceCent()) {
      throw new AppException(ErrorCode.INVALID_REQUEST, "套餐参数不合法");
    }
    repo.update(
        id,
        request.name().trim(),
        request.days(),
        request.originalPriceCent(),
        request.priceCent(),
        request.status());
  }

  @Transactional
  public void delete(long id) {
    repo.softDelete(id);
  }
}
