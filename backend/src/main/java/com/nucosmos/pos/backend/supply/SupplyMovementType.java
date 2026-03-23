package com.nucosmos.pos.backend.supply;

public enum SupplyMovementType {
    PURCHASE_IN(1),
    ADJUSTMENT_IN(1),
    ADJUSTMENT_OUT(-1),
    DAMAGE_OUT(-1),
    CONSUME_OUT(-1),
    RETURN_IN(1);

    private final int direction;

    SupplyMovementType(int direction) {
        this.direction = direction;
    }

    public int apply(int quantity) {
        return quantity * direction;
    }

    public static SupplyMovementType from(String rawValue) {
        return SupplyMovementType.valueOf(rawValue.trim().toUpperCase());
    }
}
