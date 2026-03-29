package com.nucosmos.pos.backend.order;

import com.nucosmos.pos.backend.common.exception.BadRequestException;

public enum OrderDiscountType {
    NONE,
    PERCENTAGE,
    AMOUNT,
    COMPLIMENTARY;

    public static OrderDiscountType from(String value) {
        if (value == null || value.isBlank()) {
            return NONE;
        }

        try {
            return OrderDiscountType.valueOf(value.trim().toUpperCase());
        } catch (IllegalArgumentException ex) {
            throw new BadRequestException("Unsupported discount type");
        }
    }
}
