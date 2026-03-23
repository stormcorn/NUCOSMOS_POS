package com.nucosmos.pos.backend.product.persistence;

import com.nucosmos.pos.backend.common.persistence.BaseEntity;
import com.nucosmos.pos.backend.supply.persistence.PackagingItemEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import java.math.BigDecimal;

@Entity
@Table(name = "product_recipe_version_packaging")
public class ProductRecipeVersionPackagingEntity extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "recipe_version_id", nullable = false)
    private ProductRecipeVersionEntity recipeVersion;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "packaging_item_id", nullable = false)
    private PackagingItemEntity packagingItem;

    @Column(nullable = false, precision = 10, scale = 3)
    private BigDecimal quantity;

    protected ProductRecipeVersionPackagingEntity() {
    }

    public static ProductRecipeVersionPackagingEntity create(
            ProductRecipeVersionEntity recipeVersion,
            PackagingItemEntity packagingItem,
            BigDecimal quantity
    ) {
        ProductRecipeVersionPackagingEntity entity = new ProductRecipeVersionPackagingEntity();
        entity.recipeVersion = recipeVersion;
        entity.packagingItem = packagingItem;
        entity.quantity = quantity;
        return entity;
    }

    public ProductRecipeVersionEntity getRecipeVersion() {
        return recipeVersion;
    }

    public PackagingItemEntity getPackagingItem() {
        return packagingItem;
    }

    public BigDecimal getQuantity() {
        return quantity;
    }
}
