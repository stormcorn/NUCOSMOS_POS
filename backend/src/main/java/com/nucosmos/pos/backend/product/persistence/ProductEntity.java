package com.nucosmos.pos.backend.product.persistence;

import com.nucosmos.pos.backend.common.persistence.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

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

    @Column(name = "image_url", columnDefinition = "text")
    private String imageUrl;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal price;

    @Column(name = "campaign_enabled", nullable = false)
    private boolean campaignEnabled;

    @Column(name = "campaign_label", length = 80)
    private String campaignLabel;

    @Column(name = "campaign_price", precision = 10, scale = 2)
    private BigDecimal campaignPrice;

    @Column(name = "campaign_starts_at")
    private OffsetDateTime campaignStartsAt;

    @Column(name = "campaign_ends_at")
    private OffsetDateTime campaignEndsAt;

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
            BigDecimal price,
            boolean campaignEnabled,
            String campaignLabel,
            BigDecimal campaignPrice,
            OffsetDateTime campaignStartsAt,
            OffsetDateTime campaignEndsAt
    ) {
        ProductEntity entity = new ProductEntity();
        entity.category = category;
        entity.sku = sku;
        entity.name = name;
        entity.description = normalizeDescription(description);
        entity.imageUrl = normalizeImageUrl(imageUrl);
        entity.price = price;
        entity.applyCampaignSettings(campaignEnabled, campaignLabel, campaignPrice, campaignStartsAt, campaignEndsAt);
        entity.active = true;
        return entity;
    }

    public void update(
            ProductCategoryEntity category,
            String sku,
            String name,
            String description,
            String imageUrl,
            BigDecimal price,
            boolean campaignEnabled,
            String campaignLabel,
            BigDecimal campaignPrice,
            OffsetDateTime campaignStartsAt,
            OffsetDateTime campaignEndsAt
    ) {
        this.category = category;
        this.sku = sku;
        this.name = name;
        this.description = normalizeDescription(description);
        this.imageUrl = normalizeImageUrl(imageUrl);
        this.price = price;
        this.applyCampaignSettings(campaignEnabled, campaignLabel, campaignPrice, campaignStartsAt, campaignEndsAt);
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

    public boolean isCampaignEnabled() {
        return campaignEnabled;
    }

    public String getCampaignLabel() {
        return campaignLabel;
    }

    public BigDecimal getCampaignPrice() {
        return campaignPrice;
    }

    public OffsetDateTime getCampaignStartsAt() {
        return campaignStartsAt;
    }

    public OffsetDateTime getCampaignEndsAt() {
        return campaignEndsAt;
    }

    public boolean isCampaignActive(OffsetDateTime now) {
        if (!campaignEnabled || campaignPrice == null) {
            return false;
        }

        boolean afterStart = campaignStartsAt == null || !now.isBefore(campaignStartsAt);
        boolean beforeEnd = campaignEndsAt == null || !now.isAfter(campaignEndsAt);
        return afterStart && beforeEnd;
    }

    public BigDecimal getDisplayPrice(OffsetDateTime now) {
        return isCampaignActive(now) ? campaignPrice : price;
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

    private static String normalizeCampaignLabel(String campaignLabel) {
        if (campaignLabel == null) {
            return null;
        }

        String trimmed = campaignLabel.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private void applyCampaignSettings(
            boolean campaignEnabled,
            String campaignLabel,
            BigDecimal campaignPrice,
            OffsetDateTime campaignStartsAt,
            OffsetDateTime campaignEndsAt
    ) {
        this.campaignEnabled = campaignEnabled;
        this.campaignLabel = campaignEnabled ? normalizeCampaignLabel(campaignLabel) : null;
        this.campaignPrice = campaignEnabled ? campaignPrice : null;
        this.campaignStartsAt = campaignEnabled ? campaignStartsAt : null;
        this.campaignEndsAt = campaignEnabled ? campaignEndsAt : null;
    }
}
