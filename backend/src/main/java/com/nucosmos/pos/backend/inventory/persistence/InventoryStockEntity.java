package com.nucosmos.pos.backend.inventory.persistence;

import com.nucosmos.pos.backend.common.persistence.BaseEntity;
import com.nucosmos.pos.backend.product.persistence.ProductEntity;
import com.nucosmos.pos.backend.store.persistence.StoreEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "inventory_stocks")
public class InventoryStockEntity extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "store_id", nullable = false)
    private StoreEntity store;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "product_id", nullable = false)
    private ProductEntity product;

    @Column(nullable = false)
    private int quantityOnHand;

    @Column(nullable = false)
    private int sellableQuantity;

    @Column(nullable = false)
    private int defectiveQuantity;

    @Column(nullable = false)
    private int reorderLevel;

    protected InventoryStockEntity() {
    }

    public static InventoryStockEntity create(StoreEntity store, ProductEntity product) {
        InventoryStockEntity entity = new InventoryStockEntity();
        entity.store = store;
        entity.product = product;
        entity.quantityOnHand = 0;
        entity.sellableQuantity = 0;
        entity.defectiveQuantity = 0;
        entity.reorderLevel = 0;
        return entity;
    }

    public void applySellableDelta(int delta) {
        this.sellableQuantity += delta;
        this.quantityOnHand += delta;
    }

    public void applyDefectiveDelta(int delta) {
        this.defectiveQuantity += delta;
        this.quantityOnHand += delta;
    }

    public void updateReorderLevel(int reorderLevel) {
        this.reorderLevel = reorderLevel;
    }

    public StoreEntity getStore() {
        return store;
    }

    public ProductEntity getProduct() {
        return product;
    }

    public int getQuantityOnHand() {
        return quantityOnHand;
    }

    public int getSellableQuantity() {
        return sellableQuantity;
    }

    public int getDefectiveQuantity() {
        return defectiveQuantity;
    }

    public int getReorderLevel() {
        return reorderLevel;
    }
}
