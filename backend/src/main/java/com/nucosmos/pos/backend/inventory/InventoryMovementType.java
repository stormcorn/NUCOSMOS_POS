package com.nucosmos.pos.backend.inventory;

public enum InventoryMovementType {
    PURCHASE_IN(1),
    SALE_OUT(-1),
    ADJUSTMENT_IN(1),
    ADJUSTMENT_OUT(-1),
    REFUND_IN(1);

    private final int direction;

    InventoryMovementType(int direction) {
        this.direction = direction;
    }

    public int applyDirection(int quantity) {
        return quantity * direction;
    }

    public static InventoryMovementType from(String rawValue) {
        return InventoryMovementType.valueOf(rawValue.trim().toUpperCase());
    }
}
