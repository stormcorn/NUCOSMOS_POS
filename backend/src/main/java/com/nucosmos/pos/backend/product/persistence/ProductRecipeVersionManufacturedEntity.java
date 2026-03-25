package com.nucosmos.pos.backend.product.persistence;

import com.nucosmos.pos.backend.common.persistence.BaseEntity;
import com.nucosmos.pos.backend.supply.persistence.ManufacturedItemEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import java.math.BigDecimal;

@Entity
@Table(name = "product_recipe_version_manufactured")
public class ProductRecipeVersionManufacturedEntity extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "recipe_version_id", nullable = false)
    private ProductRecipeVersionEntity recipeVersion;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "manufactured_item_id", nullable = false)
    private ManufacturedItemEntity manufacturedItem;

    @Column(nullable = false, precision = 10, scale = 3)
    private BigDecimal quantity;

    protected ProductRecipeVersionManufacturedEntity() {
    }

    public static ProductRecipeVersionManufacturedEntity create(
            ProductRecipeVersionEntity recipeVersion,
            ManufacturedItemEntity manufacturedItem,
            BigDecimal quantity
    ) {
        ProductRecipeVersionManufacturedEntity entity = new ProductRecipeVersionManufacturedEntity();
        entity.recipeVersion = recipeVersion;
        entity.manufacturedItem = manufacturedItem;
        entity.quantity = quantity;
        return entity;
    }

    public ProductRecipeVersionEntity getRecipeVersion() {
        return recipeVersion;
    }

    public ManufacturedItemEntity getManufacturedItem() {
        return manufacturedItem;
    }

    public BigDecimal getQuantity() {
        return quantity;
    }
}
