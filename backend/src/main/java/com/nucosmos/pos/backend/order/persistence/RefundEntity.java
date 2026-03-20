package com.nucosmos.pos.backend.order.persistence;

import com.nucosmos.pos.backend.auth.persistence.UserEntity;
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
@Table(name = "refunds")
public class RefundEntity extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "order_id", nullable = false)
    private OrderEntity order;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "created_by_user_id", nullable = false)
    private UserEntity createdByUser;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "payment_id", nullable = false)
    private PaymentEntity payment;

    @Column(nullable = false, length = 30)
    private String refundMethod;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal amount;

    @Column(length = 255)
    private String reason;

    @Column(nullable = false, length = 30)
    private String status;

    @Column(nullable = false)
    private OffsetDateTime refundedAt;

    protected RefundEntity() {
    }

    public RefundEntity(
            OrderEntity order,
            UserEntity createdByUser,
            PaymentEntity payment,
            String refundMethod,
            BigDecimal amount,
            String reason,
            String status,
            OffsetDateTime refundedAt
    ) {
        this.order = order;
        this.createdByUser = createdByUser;
        this.payment = payment;
        this.refundMethod = refundMethod;
        this.amount = amount;
        this.reason = reason;
        this.status = status;
        this.refundedAt = refundedAt;
    }

    public UserEntity getCreatedByUser() {
        return createdByUser;
    }

    public PaymentEntity getPayment() {
        return payment;
    }

    public String getRefundMethod() {
        return refundMethod;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public String getReason() {
        return reason;
    }

    public String getStatus() {
        return status;
    }

    public OffsetDateTime getRefundedAt() {
        return refundedAt;
    }
}
