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
@Table(name = "payments")
public class PaymentEntity extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "order_id", nullable = false)
    private OrderEntity order;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "created_by_user_id", nullable = false)
    private UserEntity createdByUser;

    @Column(nullable = false, length = 30)
    private String paymentMethod;

    @Column(nullable = false, length = 30)
    private String status;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal amount;

    @Column(precision = 10, scale = 2)
    private BigDecimal amountReceived;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal changeAmount;

    @Column(length = 50)
    private String cardTerminalProvider;

    @Column(length = 30)
    private String cardTransactionStatus;

    @Column(length = 100)
    private String cardTerminalTxnId;

    @Column(length = 50)
    private String cardApprovalCode;

    @Column(length = 30)
    private String cardMaskedPan;

    @Column(length = 50)
    private String cardBatchNo;

    @Column(length = 50)
    private String cardRrn;

    @Column(length = 30)
    private String cardEntryMode;

    @Column
    private OffsetDateTime authorizedAt;

    @Column
    private OffsetDateTime capturedAt;

    @Column
    private OffsetDateTime voidedAt;

    @Column
    private OffsetDateTime refundedAt;

    @Column(length = 255)
    private String note;

    @Column(nullable = false)
    private OffsetDateTime paidAt;

    protected PaymentEntity() {
    }

    public PaymentEntity(
            OrderEntity order,
            UserEntity createdByUser,
            String paymentMethod,
            String status,
            BigDecimal amount,
            BigDecimal amountReceived,
            BigDecimal changeAmount,
            String cardTerminalProvider,
            String cardTransactionStatus,
            String cardTerminalTxnId,
            String cardApprovalCode,
            String cardMaskedPan,
            String cardBatchNo,
            String cardRrn,
            String cardEntryMode,
            OffsetDateTime authorizedAt,
            OffsetDateTime capturedAt,
            OffsetDateTime voidedAt,
            OffsetDateTime refundedAt,
            String note,
            OffsetDateTime paidAt
    ) {
        this.order = order;
        this.createdByUser = createdByUser;
        this.paymentMethod = paymentMethod;
        this.status = status;
        this.amount = amount;
        this.amountReceived = amountReceived;
        this.changeAmount = changeAmount;
        this.cardTerminalProvider = cardTerminalProvider;
        this.cardTransactionStatus = cardTransactionStatus;
        this.cardTerminalTxnId = cardTerminalTxnId;
        this.cardApprovalCode = cardApprovalCode;
        this.cardMaskedPan = cardMaskedPan;
        this.cardBatchNo = cardBatchNo;
        this.cardRrn = cardRrn;
        this.cardEntryMode = cardEntryMode;
        this.authorizedAt = authorizedAt;
        this.capturedAt = capturedAt;
        this.voidedAt = voidedAt;
        this.refundedAt = refundedAt;
        this.note = note;
        this.paidAt = paidAt;
    }

    public OrderEntity getOrder() {
        return order;
    }

    public UserEntity getCreatedByUser() {
        return createdByUser;
    }

    public String getPaymentMethod() {
        return paymentMethod;
    }

    public String getStatus() {
        return status;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public BigDecimal getAmountReceived() {
        return amountReceived;
    }

    public BigDecimal getChangeAmount() {
        return changeAmount;
    }

    public String getCardTerminalProvider() {
        return cardTerminalProvider;
    }

    public String getCardTransactionStatus() {
        return cardTransactionStatus;
    }

    public String getCardTerminalTxnId() {
        return cardTerminalTxnId;
    }

    public String getCardApprovalCode() {
        return cardApprovalCode;
    }

    public String getCardMaskedPan() {
        return cardMaskedPan;
    }

    public String getCardBatchNo() {
        return cardBatchNo;
    }

    public String getCardRrn() {
        return cardRrn;
    }

    public String getCardEntryMode() {
        return cardEntryMode;
    }

    public OffsetDateTime getAuthorizedAt() {
        return authorizedAt;
    }

    public OffsetDateTime getCapturedAt() {
        return capturedAt;
    }

    public OffsetDateTime getVoidedAt() {
        return voidedAt;
    }

    public OffsetDateTime getRefundedAt() {
        return refundedAt;
    }

    public String getNote() {
        return note;
    }

    public OffsetDateTime getPaidAt() {
        return paidAt;
    }

    public void captureCardPayment(
            String cardTransactionStatus,
            String terminalTransactionId,
            String approvalCode,
            String maskedPan,
            String batchNo,
            String rrn,
            String entryMode,
            OffsetDateTime capturedAt,
            String note
    ) {
        this.status = "CAPTURED";
        this.cardTransactionStatus = cardTransactionStatus;
        this.cardTerminalTxnId = terminalTransactionId;
        this.cardApprovalCode = approvalCode;
        this.cardMaskedPan = maskedPan;
        this.cardBatchNo = batchNo;
        this.cardRrn = rrn;
        this.cardEntryMode = entryMode;
        this.capturedAt = capturedAt;
        this.paidAt = capturedAt;
        if (note != null && !note.isBlank()) {
            this.note = note;
        }
    }

    public void voidCardAuthorization(OffsetDateTime voidedAt) {
        if (cardTerminalProvider == null) {
            return;
        }
        this.status = "VOIDED";
        this.cardTransactionStatus = "VOIDED";
        this.voidedAt = voidedAt;
    }

    public void applyCardRefundLifecycle(OffsetDateTime refundedAt, boolean fullyRefunded) {
        if (cardTerminalProvider == null) {
            return;
        }
        if (fullyRefunded) {
            this.status = "REFUNDED";
            this.cardTransactionStatus = "REFUNDED";
            this.refundedAt = refundedAt;
        }
    }
}
