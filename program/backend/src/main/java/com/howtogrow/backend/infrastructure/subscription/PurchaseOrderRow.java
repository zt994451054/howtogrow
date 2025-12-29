package com.howtogrow.backend.infrastructure.subscription;

import java.time.Instant;

public record PurchaseOrderRow(
    long id,
    String orderNo,
    long userId,
    long planId,
    int amountCent,
    String status,
    String payTradeNo,
    String prepayId,
    Instant paidAt) {}

