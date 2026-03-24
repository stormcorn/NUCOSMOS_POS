package com.nucosmos.pos.backend.product.persistence;

import com.nucosmos.pos.backend.common.persistence.BaseEntity;
import com.nucosmos.pos.backend.product.ProductCustomizationSelectionMode;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "product_customization_groups")
public class ProductCustomizationGroupEntity extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "product_id", nullable = false)
    private ProductEntity product;

    @Column(nullable = false, length = 80)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(name = "selection_mode", nullable = false, length = 20)
    private ProductCustomizationSelectionMode selectionMode;

    @Column(nullable = false)
    private boolean required;

    @Column(name = "min_selections", nullable = false)
    private int minSelections;

    @Column(name = "max_selections", nullable = false)
    private int maxSelections;

    @Column(name = "display_order", nullable = false)
    private int displayOrder;

    @Column(nullable = false)
    private boolean active;

    protected ProductCustomizationGroupEntity() {
    }

    public static ProductCustomizationGroupEntity create(
            ProductEntity product,
            String name,
            ProductCustomizationSelectionMode selectionMode,
            boolean required,
            int minSelections,
            int maxSelections,
            int displayOrder
    ) {
        ProductCustomizationGroupEntity entity = new ProductCustomizationGroupEntity();
        entity.product = product;
        entity.name = name;
        entity.selectionMode = selectionMode;
        entity.required = required;
        entity.minSelections = minSelections;
        entity.maxSelections = maxSelections;
        entity.displayOrder = displayOrder;
        entity.active = true;
        return entity;
    }

    public ProductEntity getProduct() {
        return product;
    }

    public String getName() {
        return name;
    }

    public ProductCustomizationSelectionMode getSelectionMode() {
        return selectionMode;
    }

    public boolean isRequired() {
        return required;
    }

    public int getMinSelections() {
        return minSelections;
    }

    public int getMaxSelections() {
        return maxSelections;
    }

    public int getDisplayOrder() {
        return displayOrder;
    }

    public boolean isActive() {
        return active;
    }
}
