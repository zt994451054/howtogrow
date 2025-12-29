package com.howtogrow.backend.service.admin;

import com.howtogrow.backend.controller.admin.dto.OrderView;
import com.howtogrow.backend.controller.admin.dto.PageResponse;
import com.howtogrow.backend.infrastructure.admin.OrderQueryRepository;
import org.springframework.stereotype.Service;

@Service
public class AdminOrderService {
  private final OrderQueryRepository queryRepo;

  public AdminOrderService(OrderQueryRepository queryRepo) {
    this.queryRepo = queryRepo;
  }

  public PageResponse<OrderView> list(int page, int pageSize) {
    int offset = (page - 1) * pageSize;
    long total = queryRepo.countOrders();
    var items =
        queryRepo.listOrders(offset, pageSize).stream()
            .map(
                o ->
                    new OrderView(
                        o.id(),
                        o.orderNo(),
                        o.userId(),
                        o.planId(),
                        o.planName(),
                        o.amountCent(),
                        o.status(),
                        o.payTradeNo(),
                        o.prepayId(),
                        o.createdAt(),
                        o.paidAt()))
            .toList();
    return new PageResponse<>(page, pageSize, total, items);
  }
}
