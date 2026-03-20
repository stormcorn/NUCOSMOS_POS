package com.nucosmos.pos.backend.order;

import com.nucosmos.pos.backend.auth.AuthenticatedUser;
import com.nucosmos.pos.backend.order.persistence.OrderEntity;

public record CardVoidCommand(
        OrderEntity order,
        AuthenticatedUser user,
        String terminalTransactionId,
        String reason
) {
}
