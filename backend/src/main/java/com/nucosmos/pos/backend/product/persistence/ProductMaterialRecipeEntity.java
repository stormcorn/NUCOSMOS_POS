package com.nucosmos.pos.backend.product.persistence;

import com.nucosmos.pos.backend.common.persistence.BaseEntity;
import com.nucosmos.pos.backend.supply.persistence.MaterialItemEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import java.math.BigDecimal;

@Entity
@Table(name = "product_material_recipes")
public class ProductMaterialRecipeEntity extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "product_id", nullable = false)
    private ProductEntity product;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "material_item_id", nullable = false)
    private MaterialItemEntity materialItem;

    @Column(nullable = false, precision = 10, scale = 3)
    private BigDecimal quantity;

    protected ProductMaterialRecipeEntity() {
    }

    public static ProductMaterialRecipeEntity create(
            ProductEntity product,
            MaterialItemEntity materialItem,
            BigDecimal quantity
    ) {
        ProductMaterialRecipeEntity entity = new ProductMaterialRecipeEntity();
        entity.product = product;
        entity.materialItem = materialItem;
        entity.quantity = quantity;
        return entity;
    }

    public ProductEntity getProduct() {
        return product;
    }

    public MaterialItemEntity getMaterialItem() {
        return materialItem;
    }

    public BigDecimal getQuantity() {
        return quantity;
    }
}
