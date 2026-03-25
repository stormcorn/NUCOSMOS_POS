package com.nucosmos.pos.backend.supply.persistence;

import com.nucosmos.pos.backend.common.persistence.BaseEntity;
import com.nucosmos.pos.backend.store.persistence.StoreEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import java.math.BigDecimal;

@Entity
@Table(name = "manufactured_items")
public class ManufacturedItemEntity extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "store_id", nullable = false)
    private StoreEntity store;

    @Column(nullable = false, length = 50)
    private String sku;

    @Column(nullable = false, length = 120)
    private String name;

    @Column(nullable = false, length = 30)
    private String unit;

    @Column(nullable = false, length = 30)
    private String purchaseUnit;

    @Column(nullable = false)
    private int purchaseToStockRatio;

    @Column(name = "image_url", columnDefinition = "text")
    private String imageUrl;

    @Column(length = 500)
    private String description;

    @Column(nullable = false)
    private int quantityOnHand;

    @Column(nullable = false)
    private int reorderLevel;

    @Column(precision = 10, scale = 2)
    private BigDecimal latestUnitCost;

    @Column(nullable = false)
    private boolean active;

    protected ManufacturedItemEntity() {
    }

    public static ManufacturedItemEntity create(
            StoreEntity store,
            String sku,
            String name,
            String unit,
            String purchaseUnit,
            int purchaseToStockRatio,
            String imageUrl,
            String description,
            int reorderLevel,
            BigDecimal latestUnitCost
    ) {
        ManufacturedItemEntity entity = new ManufacturedItemEntity();
        entity.store = store;
        entity.sku = sku;
        entity.name = name;
        entity.unit = unit;
        entity.purchaseUnit = purchaseUnit;
        entity.purchaseToStockRatio = purchaseToStockRatio;
        entity.imageUrl = normalize(imageUrl);
        entity.description = normalize(description);
        entity.quantityOnHand = 0;
        entity.reorderLevel = reorderLevel;
        entity.latestUnitCost = latestUnitCost;
        entity.active = true;
        return entity;
    }

    public void update(
            String sku,
            String name,
            String unit,
            String purchaseUnit,
            int purchaseToStockRatio,
            String imageUrl,
            String description,
            int reorderLevel,
            BigDecimal latestUnitCost
    ) {
        this.sku = sku;
        this.name = name;
        this.unit = unit;
        this.purchaseUnit = purchaseUnit;
        this.purchaseToStockRatio = purchaseToStockRatio;
        this.imageUrl = normalize(imageUrl);
        this.description = normalize(description);
        this.reorderLevel = reorderLevel;
        this.latestUnitCost = latestUnitCost;
    }

    public void applyMovement(int delta, BigDecimal nextUnitCost) {
        this.quantityOnHand += delta;
        if (nextUnitCost != null) {
            this.latestUnitCost = nextUnitCost;
        }
    }

    public void deactivate() {
        this.active = false;
    }

    public StoreEntity getStore() {
        return store;
    }

    public String getSku() {
        return sku;
    }

    public String getName() {
        return name;
    }

    public String getUnit() {
        return unit;
    }

    public String getPurchaseUnit() {
        return purchaseUnit;
    }

    public int getPurchaseToStockRatio() {
        return purchaseToStockRatio;
    }

    public String getImageUrl() {
        return imageUrl;
    }

    public String getDescription() {
        return description;
    }

    public int getQuantityOnHand() {
        return quantityOnHand;
    }

    public int getReorderLevel() {
        return reorderLevel;
    }

    public BigDecimal getLatestUnitCost() {
        return latestUnitCost;
    }

    public boolean isActive() {
        return active;
    }

    private static String normalize(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
