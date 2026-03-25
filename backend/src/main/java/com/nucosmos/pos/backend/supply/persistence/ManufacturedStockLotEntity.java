package com.nucosmos.pos.backend.supply.persistence;

import com.nucosmos.pos.backend.common.persistence.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "manufactured_stock_lots")
public class ManufacturedStockLotEntity extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "manufactured_item_id", nullable = false)
    private ManufacturedItemEntity manufacturedItem;

    @Column(nullable = false, length = 30)
    private String sourceType;

    @Column
    private UUID sourceId;

    @Column(length = 80)
    private String batchCode;

    @Column
    private OffsetDateTime expiryDate;

    @Column
    private OffsetDateTime manufacturedAt;

    @Column(nullable = false)
    private int receivedQuantity;

    @Column(nullable = false)
    private int remainingQuantity;

    @Column(precision = 10, scale = 2)
    private BigDecimal unitCost;

    @Column(nullable = false)
    private OffsetDateTime receivedAt;

    protected ManufacturedStockLotEntity() {
    }

    public static ManufacturedStockLotEntity create(
            ManufacturedItemEntity manufacturedItem,
            String sourceType,
            UUID sourceId,
            String batchCode,
            OffsetDateTime expiryDate,
            OffsetDateTime manufacturedAt,
            int receivedQuantity,
            BigDecimal unitCost,
            OffsetDateTime receivedAt
    ) {
        ManufacturedStockLotEntity entity = new ManufacturedStockLotEntity();
        entity.manufacturedItem = manufacturedItem;
        entity.sourceType = sourceType;
        entity.sourceId = sourceId;
        entity.batchCode = normalize(batchCode);
        entity.expiryDate = expiryDate;
        entity.manufacturedAt = manufacturedAt;
        entity.receivedQuantity = receivedQuantity;
        entity.remainingQuantity = receivedQuantity;
        entity.unitCost = unitCost;
        entity.receivedAt = receivedAt;
        return entity;
    }

    public int consume(int quantity) {
        int consumed = Math.min(quantity, remainingQuantity);
        remainingQuantity -= consumed;
        return consumed;
    }

    public void restore(int quantity) {
        remainingQuantity += quantity;
        if (remainingQuantity > receivedQuantity) {
            remainingQuantity = receivedQuantity;
        }
    }

    public ManufacturedItemEntity getManufacturedItem() {
        return manufacturedItem;
    }

    public String getSourceType() {
        return sourceType;
    }

    public UUID getSourceId() {
        return sourceId;
    }

    public String getBatchCode() {
        return batchCode;
    }

    public OffsetDateTime getExpiryDate() {
        return expiryDate;
    }

    public OffsetDateTime getManufacturedAt() {
        return manufacturedAt;
    }

    public int getReceivedQuantity() {
        return receivedQuantity;
    }

    public int getRemainingQuantity() {
        return remainingQuantity;
    }

    public BigDecimal getUnitCost() {
        return unitCost;
    }

    public OffsetDateTime getReceivedAt() {
        return receivedAt;
    }

    private static String normalize(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
