package com.nucosmos.pos.backend.inventory.persistence;

import com.nucosmos.pos.backend.auth.persistence.UserEntity;
import com.nucosmos.pos.backend.common.persistence.BaseEntity;
import com.nucosmos.pos.backend.product.persistence.ProductEntity;
import com.nucosmos.pos.backend.store.persistence.StoreEntity;
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
@Table(name = "inventory_movements")
public class InventoryMovementEntity extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "store_id", nullable = false)
    private StoreEntity store;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "product_id", nullable = false)
    private ProductEntity product;

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

    @Column(length = 30)
    private String referenceType;

    @Column
    private UUID referenceId;

    @Column(nullable = false)
    private OffsetDateTime occurredAt;

    protected InventoryMovementEntity() {
    }

    public InventoryMovementEntity(
            StoreEntity store,
            ProductEntity product,
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
        this.store = store;
        this.product = product;
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

    public ProductEntity getProduct() {
        return product;
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
