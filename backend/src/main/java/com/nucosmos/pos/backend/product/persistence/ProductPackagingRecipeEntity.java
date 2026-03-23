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
@Table(name = "product_packaging_recipes")
public class ProductPackagingRecipeEntity extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "product_id", nullable = false)
    private ProductEntity product;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "packaging_item_id", nullable = false)
    private PackagingItemEntity packagingItem;

    @Column(nullable = false, precision = 10, scale = 3)
    private BigDecimal quantity;

    protected ProductPackagingRecipeEntity() {
    }

    public static ProductPackagingRecipeEntity create(
            ProductEntity product,
            PackagingItemEntity packagingItem,
            BigDecimal quantity
    ) {
        ProductPackagingRecipeEntity entity = new ProductPackagingRecipeEntity();
        entity.product = product;
        entity.packagingItem = packagingItem;
        entity.quantity = quantity;
        return entity;
    }

    public ProductEntity getProduct() {
        return product;
    }

    public PackagingItemEntity getPackagingItem() {
        return packagingItem;
    }

    public BigDecimal getQuantity() {
        return quantity;
    }
}
