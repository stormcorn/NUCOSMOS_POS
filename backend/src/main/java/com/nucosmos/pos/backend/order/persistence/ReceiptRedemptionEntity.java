package com.nucosmos.pos.backend.order.persistence;

import com.nucosmos.pos.backend.common.persistence.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
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

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "claimed_member_id")
    private ReceiptMemberEntity claimedMember;

    @Column(length = 20)
    private String drawOutcome;

    @Column(nullable = false)
    private int awardedPoints;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "prize_id")
    private ReceiptPrizeEntity prize;

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

    public ReceiptMemberEntity getClaimedMember() {
        return claimedMember;
    }

    public String getDrawOutcome() {
        return drawOutcome;
    }

    public int getAwardedPoints() {
        return awardedPoints;
    }

    public ReceiptPrizeEntity getPrize() {
        return prize;
    }

    public boolean isClaimed() {
        return claimedAt != null;
    }

    public void markClaimed(
            OffsetDateTime claimedAt,
            ReceiptMemberEntity claimedMember,
            String drawOutcome,
            int awardedPoints,
            ReceiptPrizeEntity prize
    ) {
        this.claimedAt = claimedAt;
        this.claimedMember = claimedMember;
        this.drawOutcome = drawOutcome;
        this.awardedPoints = awardedPoints;
        this.prize = prize;
    }
}
