package com.nucosmos.pos.backend.order;

import com.nucosmos.pos.backend.auth.AuthenticatedUser;
import com.nucosmos.pos.backend.order.persistence.OrderEntity;
import com.nucosmos.pos.backend.order.persistence.PaymentEntity;

import java.math.BigDecimal;

public record CardRefundCommand(
        OrderEntity order,
        PaymentEntity payment,
        AuthenticatedUser user,
        BigDecimal amount,
        RefundMethod refundMethod,
        String reason
) {
}
