package com.nucosmos.pos.backend.order;

import com.nucosmos.pos.backend.auth.AuthenticatedUser;
import com.nucosmos.pos.backend.order.persistence.OrderEntity;

import java.math.BigDecimal;

public record CardCaptureCommand(
        OrderEntity order,
        AuthenticatedUser user,
        BigDecimal amount,
        String terminalTransactionId
) {
}
