package com.nucosmos.pos.backend.order.persistence;

import com.nucosmos.pos.backend.common.persistence.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

@Entity
@Table(name = "receipt_coupons")
public class ReceiptCouponEntity extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "member_id", nullable = false)
    private ReceiptMemberEntity member;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "source_redemption_id", nullable = false, unique = true)
    private ReceiptRedemptionEntity sourceRedemption;

    @Column(nullable = false, unique = true, length = 24)
    private String couponCode;

    @Column(nullable = false, length = 120)
    private String title;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal discountAmount;

    @Column(nullable = false, length = 20)
    private String status;

    @Column(nullable = false)
    private OffsetDateTime issuedAt;

    protected ReceiptCouponEntity() {
    }

    public ReceiptCouponEntity(
            ReceiptMemberEntity member,
            ReceiptRedemptionEntity sourceRedemption,
            String couponCode,
            String title,
            BigDecimal discountAmount,
            String status,
            OffsetDateTime issuedAt
    ) {
        this.member = member;
        this.sourceRedemption = sourceRedemption;
        this.couponCode = couponCode;
        this.title = title;
        this.discountAmount = discountAmount;
        this.status = status;
        this.issuedAt = issuedAt;
    }

    public String getCouponCode() {
        return couponCode;
    }

    public String getTitle() {
        return title;
    }

    public BigDecimal getDiscountAmount() {
        return discountAmount;
    }

    public String getStatus() {
        return status;
    }

    public OffsetDateTime getIssuedAt() {
        return issuedAt;
    }
}
