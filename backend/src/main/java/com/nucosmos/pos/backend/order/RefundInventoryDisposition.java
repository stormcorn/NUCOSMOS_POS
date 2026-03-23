package com.nucosmos.pos.backend.order;

import com.nucosmos.pos.backend.common.exception.BadRequestException;

public enum RefundInventoryDisposition {
    SELLABLE,
    DEFECTIVE;

    public static RefundInventoryDisposition from(String value) {
        if (value == null || value.isBlank()) {
            return SELLABLE;
        }

        try {
            return RefundInventoryDisposition.valueOf(value.trim().toUpperCase());
        } catch (IllegalArgumentException error) {
            throw new BadRequestException("Unsupported refund inventory disposition");
        }
    }
}
