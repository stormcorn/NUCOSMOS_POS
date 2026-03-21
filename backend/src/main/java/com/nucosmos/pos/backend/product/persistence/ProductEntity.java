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
@Table(name = "products")
public class ProductEntity extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "category_id", nullable = false)
    private ProductCategoryEntity category;

    @Column(nullable = false, unique = true, length = 50)
    private String sku;

    @Column(nullable = false, length = 120)
    private String name;

    @Column(length = 500)
    private String description;

    @Column(name = "image_url", length = 500)
    private String imageUrl;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal price;

    @Column(nullable = false)
    private boolean active;

    protected ProductEntity() {
    }

    public static ProductEntity create(
            ProductCategoryEntity category,
            String sku,
            String name,
            String description,
            String imageUrl,
            BigDecimal price
    ) {
        ProductEntity entity = new ProductEntity();
        entity.category = category;
        entity.sku = sku;
        entity.name = name;
        entity.description = normalizeDescription(description);
        entity.imageUrl = normalizeImageUrl(imageUrl);
        entity.price = price;
        entity.active = true;
        return entity;
    }

    public void update(
            ProductCategoryEntity category,
            String sku,
            String name,
            String description,
            String imageUrl,
            BigDecimal price
    ) {
        this.category = category;
        this.sku = sku;
        this.name = name;
        this.description = normalizeDescription(description);
        this.imageUrl = normalizeImageUrl(imageUrl);
        this.price = price;
    }

    public void deactivate() {
        this.active = false;
    }

    public ProductCategoryEntity getCategory() {
        return category;
    }

    public String getSku() {
        return sku;
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public BigDecimal getPrice() {
        return price;
    }

    public String getImageUrl() {
        return imageUrl;
    }

    public boolean isActive() {
        return active;
    }

    private static String normalizeDescription(String description) {
        if (description == null) {
            return null;
        }
        String trimmed = description.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private static String normalizeImageUrl(String imageUrl) {
        if (imageUrl == null) {
            return null;
        }
        String trimmed = imageUrl.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
