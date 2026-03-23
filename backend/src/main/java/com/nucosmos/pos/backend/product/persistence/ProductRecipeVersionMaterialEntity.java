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
@Table(name = "product_recipe_version_materials")
public class ProductRecipeVersionMaterialEntity extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "recipe_version_id", nullable = false)
    private ProductRecipeVersionEntity recipeVersion;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "material_item_id", nullable = false)
    private MaterialItemEntity materialItem;

    @Column(nullable = false, precision = 10, scale = 3)
    private BigDecimal quantity;

    protected ProductRecipeVersionMaterialEntity() {
    }

    public static ProductRecipeVersionMaterialEntity create(
            ProductRecipeVersionEntity recipeVersion,
            MaterialItemEntity materialItem,
            BigDecimal quantity
    ) {
        ProductRecipeVersionMaterialEntity entity = new ProductRecipeVersionMaterialEntity();
        entity.recipeVersion = recipeVersion;
        entity.materialItem = materialItem;
        entity.quantity = quantity;
        return entity;
    }

    public ProductRecipeVersionEntity getRecipeVersion() {
        return recipeVersion;
    }

    public MaterialItemEntity getMaterialItem() {
        return materialItem;
    }

    public BigDecimal getQuantity() {
        return quantity;
    }
}
