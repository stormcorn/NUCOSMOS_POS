package com.nucosmos.pos.backend.product.persistence;

import com.nucosmos.pos.backend.common.persistence.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import java.math.BigDecimal;

@Entity
@Table(name = "product_customization_options")
public class ProductCustomizationOptionEntity extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "customization_group_id", nullable = false)
    private ProductCustomizationGroupEntity customizationGroup;

    @Column(nullable = false, length = 80)
    private String name;

    @Column(name = "price_delta", nullable = false, precision = 10, scale = 2)
    private BigDecimal priceDelta;

    @Column(name = "default_selected", nullable = false)
    private boolean defaultSelected;

    @Column(name = "display_order", nullable = false)
    private int displayOrder;

    @Column(nullable = false)
    private boolean active;

    protected ProductCustomizationOptionEntity() {
    }

    public static ProductCustomizationOptionEntity create(
            ProductCustomizationGroupEntity customizationGroup,
            String name,
            BigDecimal priceDelta,
            boolean defaultSelected,
            int displayOrder
    ) {
        ProductCustomizationOptionEntity entity = new ProductCustomizationOptionEntity();
        entity.customizationGroup = customizationGroup;
        entity.name = name;
        entity.priceDelta = priceDelta;
        entity.defaultSelected = defaultSelected;
        entity.displayOrder = displayOrder;
        entity.active = true;
        return entity;
    }

    public ProductCustomizationGroupEntity getCustomizationGroup() {
        return customizationGroup;
    }

    public String getName() {
        return name;
    }

    public BigDecimal getPriceDelta() {
        return priceDelta;
    }

    public boolean isDefaultSelected() {
        return defaultSelected;
    }

    public int getDisplayOrder() {
        return displayOrder;
    }

    public boolean isActive() {
        return active;
    }
}
