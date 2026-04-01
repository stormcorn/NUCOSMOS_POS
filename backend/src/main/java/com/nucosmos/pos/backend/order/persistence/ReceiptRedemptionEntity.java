package com.nucosmos.pos.backend.order.persistence;

import com.nucosmos.pos.backend.common.persistence.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;

import java.time.OffsetDateTime;

@Entity
@Table(name = "receipt_redemptions")
public class ReceiptRedemptionEntity extends BaseEntity {

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "order_id", nullable = false, unique = true)
    private OrderEntity order;

    @Column(nullable = false, unique = true, length = 80)
    private String publicToken;

    @Column(nullable = false, unique = true, length = 20)
    private String claimCode;

    @Column
    private OffsetDateTime claimedAt;

    protected ReceiptRedemptionEntity() {
    }

    public ReceiptRedemptionEntity(OrderEntity order, String publicToken, String claimCode) {
        this.order = order;
        this.publicToken = publicToken;
        this.claimCode = claimCode;
    }

    public OrderEntity getOrder() {
        return order;
    }

    public String getPublicToken() {
        return publicToken;
    }

    public String getClaimCode() {
        return claimCode;
    }

    public OffsetDateTime getClaimedAt() {
        return claimedAt;
    }

    public boolean isClaimed() {
        return claimedAt != null;
    }

    public void markClaimed(OffsetDateTime claimedAt) {
        this.claimedAt = claimedAt;
    }
}
