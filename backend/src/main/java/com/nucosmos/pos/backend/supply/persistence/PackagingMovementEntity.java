package com.nucosmos.pos.backend.supply.persistence;

import com.nucosmos.pos.backend.auth.persistence.UserEntity;
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
@Table(name = "packaging_movements")
public class PackagingMovementEntity extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "packaging_item_id", nullable = false)
    private PackagingItemEntity packagingItem;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by_user_id")
    private UserEntity createdByUser;

    @Column(nullable = false, length = 30)
    private String movementType;

    @Column(nullable = false)
    private int quantity;

    @Column(nullable = false)
    private int quantityDelta;

    @Column(nullable = false)
    private int quantityAfter;

    @Column(precision = 12, scale = 6)
    private BigDecimal unitCost;

    @Column(length = 255)
    private String note;

    @Column(length = 30)
    private String referenceType;

    @Column
    private UUID referenceId;

    @Column(nullable = false)
    private OffsetDateTime occurredAt;

    protected PackagingMovementEntity() {
    }

    public PackagingMovementEntity(
            PackagingItemEntity packagingItem,
            UserEntity createdByUser,
            String movementType,
            int quantity,
            int quantityDelta,
            int quantityAfter,
            BigDecimal unitCost,
            String note,
            String referenceType,
            UUID referenceId,
            OffsetDateTime occurredAt
    ) {
        this.packagingItem = packagingItem;
        this.createdByUser = createdByUser;
        this.movementType = movementType;
        this.quantity = quantity;
        this.quantityDelta = quantityDelta;
        this.quantityAfter = quantityAfter;
        this.unitCost = unitCost;
        this.note = note;
        this.referenceType = referenceType;
        this.referenceId = referenceId;
        this.occurredAt = occurredAt;
    }

    public PackagingItemEntity getPackagingItem() {
        return packagingItem;
    }

    public String getMovementType() {
        return movementType;
    }

    public int getQuantity() {
        return quantity;
    }

    public int getQuantityDelta() {
        return quantityDelta;
    }

    public int getQuantityAfter() {
        return quantityAfter;
    }

    public BigDecimal getUnitCost() {
        return unitCost;
    }

    public String getNote() {
        return note;
    }

    public String getReferenceType() {
        return referenceType;
    }

    public UUID getReferenceId() {
        return referenceId;
    }

    public OffsetDateTime getOccurredAt() {
        return occurredAt;
    }
}
