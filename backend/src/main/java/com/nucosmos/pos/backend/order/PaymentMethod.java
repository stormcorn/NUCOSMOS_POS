package com.nucosmos.pos.backend.order;

import com.nucosmos.pos.backend.common.exception.BadRequestException;

public enum PaymentMethod {
    CASH,
    CARD,
    OTHER;

    public static PaymentMethod from(String value) {
        if (value == null || value.isBlank()) {
            throw new BadRequestException("Payment method is required");
        }

        try {
            return PaymentMethod.valueOf(value.trim().toUpperCase());
        } catch (IllegalArgumentException ex) {
            throw new BadRequestException("Unsupported payment method");
        }
    }
}
