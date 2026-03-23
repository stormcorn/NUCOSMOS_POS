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

@Entity
@Table(name = "material_movements")
public class MaterialMovementEntity extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "material_id", nullable = false)
    private MaterialItemEntity material;

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

    @Column(precision = 10, scale = 2)
    private BigDecimal unitCost;

    @Column(length = 255)
    private String note;

    @Column(nullable = false)
    private OffsetDateTime occurredAt;

    protected MaterialMovementEntity() {
    }

    public MaterialMovementEntity(
            MaterialItemEntity material,
            UserEntity createdByUser,
            String movementType,
            int quantity,
            int quantityDelta,
            int quantityAfter,
            BigDecimal unitCost,
            String note,
            OffsetDateTime occurredAt
    ) {
        this.material = material;
        this.createdByUser = createdByUser;
        this.movementType = movementType;
        this.quantity = quantity;
        this.quantityDelta = quantityDelta;
        this.quantityAfter = quantityAfter;
        this.unitCost = unitCost;
        this.note = note;
        this.occurredAt = occurredAt;
    }

    public MaterialItemEntity getMaterial() {
        return material;
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

    public OffsetDateTime getOccurredAt() {
        return occurredAt;
    }
}
