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
@Table(name = "product_manufactured_recipes")
public class ProductManufacturedRecipeEntity extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "product_id", nullable = false)
    private ProductEntity product;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "manufactured_item_id", nullable = false)
    private ManufacturedItemEntity manufacturedItem;

    @Column(nullable = false, precision = 10, scale = 3)
    private BigDecimal quantity;

    protected ProductManufacturedRecipeEntity() {
    }

    public static ProductManufacturedRecipeEntity create(
            ProductEntity product,
            ManufacturedItemEntity manufacturedItem,
            BigDecimal quantity
    ) {
        ProductManufacturedRecipeEntity entity = new ProductManufacturedRecipeEntity();
        entity.product = product;
        entity.manufacturedItem = manufacturedItem;
        entity.quantity = quantity;
        return entity;
    }

    public ProductEntity getProduct() {
        return product;
    }

    public ManufacturedItemEntity getManufacturedItem() {
        return manufacturedItem;
    }

    public BigDecimal getQuantity() {
        return quantity;
    }
}
