package com.nucosmos.pos.backend.product.persistence;

import com.nucosmos.pos.backend.common.persistence.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import java.time.OffsetDateTime;

@Entity
@Table(name = "product_recipe_versions")
public class ProductRecipeVersionEntity extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "product_id", nullable = false)
    private ProductEntity product;

    @Column(nullable = false)
    private int versionNumber;

    @Column(nullable = false, length = 20)
    private String status;

    @Column(length = 255)
    private String note;

    @Column(nullable = false)
    private OffsetDateTime effectiveAt;

    protected ProductRecipeVersionEntity() {
    }

    public static ProductRecipeVersionEntity create(
            ProductEntity product,
            int versionNumber,
            String note,
            OffsetDateTime effectiveAt
    ) {
        ProductRecipeVersionEntity entity = new ProductRecipeVersionEntity();
        entity.product = product;
        entity.versionNumber = versionNumber;
        entity.status = "ACTIVE";
        entity.note = normalize(note);
        entity.effectiveAt = effectiveAt;
        return entity;
    }

    public void archive() {
        this.status = "ARCHIVED";
    }

    public ProductEntity getProduct() {
        return product;
    }

    public int getVersionNumber() {
        return versionNumber;
    }

    public String getStatus() {
        return status;
    }

    public String getNote() {
        return note;
    }

    public OffsetDateTime getEffectiveAt() {
        return effectiveAt;
    }

    private static String normalize(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
