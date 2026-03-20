package com.nucosmos.pos.backend.order;

import com.nucosmos.pos.backend.common.exception.BadRequestException;

public enum RefundMethod {
    CASH,
    CARD_REVERSAL,
    CARD_REFUND;

    public static RefundMethod from(String value) {
        if (value == null || value.isBlank()) {
            throw new BadRequestException("Refund method is required");
        }

        try {
            return RefundMethod.valueOf(value.trim().toUpperCase());
        } catch (IllegalArgumentException ex) {
            throw new BadRequestException("Unsupported refund method");
        }
    }
}
