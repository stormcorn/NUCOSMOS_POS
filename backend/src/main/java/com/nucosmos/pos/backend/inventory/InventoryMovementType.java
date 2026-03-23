package com.nucosmos.pos.backend.inventory;

public enum InventoryMovementType {
    PURCHASE_IN(InventoryStockBucket.SELLABLE, 1),
    SALE_OUT(InventoryStockBucket.SELLABLE, -1),
    REFUND_IN(InventoryStockBucket.SELLABLE, 1),
    REFUND_DEFECT(InventoryStockBucket.DEFECTIVE, 1),
    DEFECTIVE_RESTORE(InventoryStockBucket.DEFECTIVE, 0),
    ADJUSTMENT_IN(InventoryStockBucket.SELLABLE, 1),
    ADJUSTMENT_OUT(InventoryStockBucket.SELLABLE, -1),
    DAMAGE_OUT(InventoryStockBucket.SELLABLE, -1),
    SCRAP_OUT(InventoryStockBucket.DEFECTIVE, -1),
    SAMPLE_OUT(InventoryStockBucket.SELLABLE, -1),
    PRODUCTION_CONSUME(InventoryStockBucket.SELLABLE, -1);

    private final InventoryStockBucket stockBucket;
    private final int direction;

    InventoryMovementType(InventoryStockBucket stockBucket, int direction) {
        this.stockBucket = stockBucket;
        this.direction = direction;
    }

    public InventoryStockBucket stockBucket() {
        return stockBucket;
    }

    public int applyDirection(int quantity) {
        return quantity * direction;
    }

    public static InventoryMovementType from(String rawValue) {
        return InventoryMovementType.valueOf(rawValue.trim().toUpperCase());
    }
}
