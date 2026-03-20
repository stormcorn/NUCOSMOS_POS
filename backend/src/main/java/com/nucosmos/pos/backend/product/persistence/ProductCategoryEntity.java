package com.nucosmos.pos.backend.product.persistence;

import com.nucosmos.pos.backend.common.persistence.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

@Entity
@Table(name = "product_categories")
public class ProductCategoryEntity extends BaseEntity {

    @Column(nullable = false, unique = true, length = 50)
    private String code;

    @Column(nullable = false, length = 120)
    private String name;

    @Column(nullable = false)
    private Integer displayOrder;

    @Column(nullable = false)
    private boolean active;

    public String getCode() {
        return code;
    }

    public String getName() {
        return name;
    }

    public Integer getDisplayOrder() {
        return displayOrder;
    }

    public boolean isActive() {
        return active;
    }
}
