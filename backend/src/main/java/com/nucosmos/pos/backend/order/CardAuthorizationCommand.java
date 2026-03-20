package com.nucosmos.pos.backend.order;

import com.nucosmos.pos.backend.auth.AuthenticatedUser;
import com.nucosmos.pos.backend.order.persistence.OrderEntity;

import java.math.BigDecimal;

public record CardAuthorizationCommand(
        OrderEntity order,
        AuthenticatedUser user,
        BigDecimal amount
) {
}
