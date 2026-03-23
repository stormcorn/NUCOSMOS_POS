package com.nucosmos.pos.backend.inventory.stocktake.persistence;

import com.nucosmos.pos.backend.common.persistence.BaseEntity;
import com.nucosmos.pos.backend.product.persistence.ProductEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "inventory_stocktake_items")
public class InventoryStocktakeItemEntity extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "stocktake_id", nullable = false)
    private InventoryStocktakeEntity stocktake;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "product_id", nullable = false)
    private ProductEntity product;

    @Column(nullable = false)
    private int expectedSellableQuantity;

    @Column(nullable = false)
    private int countedSellableQuantity;

    @Column(nullable = false)
    private int varianceQuantity;

    @Column(length = 50)
    private String reasonCode;

    @Column(length = 255)
    private String note;

    protected InventoryStocktakeItemEntity() {
    }

    public InventoryStocktakeItemEntity(
            InventoryStocktakeEntity stocktake,
            ProductEntity product,
            int expectedSellableQuantity,
            int countedSellableQuantity,
            int varianceQuantity,
            String reasonCode,
            String note
    ) {
        this.stocktake = stocktake;
        this.product = product;
        this.expectedSellableQuantity = expectedSellableQuantity;
        this.countedSellableQuantity = countedSellableQuantity;
        this.varianceQuantity = varianceQuantity;
        this.reasonCode = reasonCode;
        this.note = note;
    }

    public ProductEntity getProduct() {
        return product;
    }

    public int getExpectedSellableQuantity() {
        return expectedSellableQuantity;
    }

    public int getCountedSellableQuantity() {
        return countedSellableQuantity;
    }

    public int getVarianceQuantity() {
        return varianceQuantity;
    }

    public String getReasonCode() {
        return reasonCode;
    }

    public String getNote() {
        return note;
    }
}
