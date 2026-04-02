package com.nucosmos.pos.backend.order.persistence;

import com.nucosmos.pos.backend.common.persistence.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

import java.math.BigDecimal;

@Entity
@Table(name = "receipt_prizes")
public class ReceiptPrizeEntity extends BaseEntity {

    @Column(nullable = false, length = 120)
    private String name;

    @Column(length = 240)
    private String description;

    @Column(nullable = false, precision = 5, scale = 2)
    private BigDecimal probabilityPercent;

    @Column(nullable = false)
    private int remainingQuantity;

    @Column(nullable = false)
    private boolean active;

    @Column(nullable = false)
    private int displayOrder;

    protected ReceiptPrizeEntity() {
    }

    public ReceiptPrizeEntity(
            String name,
            String description,
            BigDecimal probabilityPercent,
            int remainingQuantity,
            boolean active,
            int displayOrder
    ) {
        this.name = name;
        this.description = description;
        this.probabilityPercent = probabilityPercent;
        this.remainingQuantity = remainingQuantity;
        this.active = active;
        this.displayOrder = displayOrder;
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public BigDecimal getProbabilityPercent() {
        return probabilityPercent;
    }

    public int getRemainingQuantity() {
        return remainingQuantity;
    }

    public boolean isActive() {
        return active;
    }

    public int getDisplayOrder() {
        return displayOrder;
    }

    public void update(
            String name,
            String description,
            BigDecimal probabilityPercent,
            int remainingQuantity,
            boolean active,
            int displayOrder
    ) {
        this.name = name;
        this.description = description;
        this.probabilityPercent = probabilityPercent;
        this.remainingQuantity = remainingQuantity;
        this.active = active;
        this.displayOrder = displayOrder;
    }

    public boolean canDraw() {
        return active && remainingQuantity > 0 && probabilityPercent.compareTo(BigDecimal.ZERO) > 0;
    }

    public void decrementQuantity() {
        if (remainingQuantity > 0) {
            remainingQuantity -= 1;
        }
    }
}
