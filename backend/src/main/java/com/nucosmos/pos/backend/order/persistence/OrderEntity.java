package com.nucosmos.pos.backend.order.persistence;

import com.nucosmos.pos.backend.auth.persistence.UserEntity;
import com.nucosmos.pos.backend.common.persistence.BaseEntity;
import com.nucosmos.pos.backend.device.persistence.DeviceEntity;
import com.nucosmos.pos.backend.store.persistence.StoreEntity;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OrderBy;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "orders")
public class OrderEntity extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "store_id", nullable = false)
    private StoreEntity store;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "device_id")
    private DeviceEntity device;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "created_by_user_id", nullable = false)
    private UserEntity createdByUser;

    @Column(nullable = false, unique = true, length = 50)
    private String orderNumber;

    @Column(nullable = false, length = 30)
    private String status;

    @Column(nullable = false, length = 30)
    private String paymentStatus;

    @Column(nullable = false)
    private int itemCount;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal subtotalAmount;

    @Column(nullable = false, length = 30)
    private String discountType;

    @Column(precision = 10, scale = 2)
    private BigDecimal discountValue;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal discountAmount;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal totalAmount;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal paidAmount;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal changeAmount;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal refundedAmount;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal cogsAmount;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal refundedCogsAmount;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal netCogsAmount;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal grossProfitAmount;

    @Column(length = 500)
    private String note;

    @Column(length = 255)
    private String discountNote;

    @Column(nullable = false)
    private OffsetDateTime orderedAt;

    @Column
    private OffsetDateTime closedAt;

    @Column
    private OffsetDateTime voidedAt;

    @Column(length = 255)
    private String voidNote;

    @Column(nullable = false)
    private boolean inventoryCommitted;

    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("lineNumber ASC")
    private List<OrderItemEntity> items = new ArrayList<>();

    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("paidAt ASC")
    private List<PaymentEntity> payments = new ArrayList<>();

    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("refundedAt ASC")
    private List<RefundEntity> refunds = new ArrayList<>();

    @OneToOne(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private ReceiptRedemptionEntity receiptRedemption;

    protected OrderEntity() {
    }

    public OrderEntity(
            StoreEntity store,
            DeviceEntity device,
            UserEntity createdByUser,
            String orderNumber,
            String status,
            String paymentStatus,
            int itemCount,
            BigDecimal subtotalAmount,
            String discountType,
            BigDecimal discountValue,
            BigDecimal discountAmount,
            BigDecimal totalAmount,
            BigDecimal paidAmount,
            BigDecimal changeAmount,
            BigDecimal refundedAmount,
            String note,
            String discountNote,
            OffsetDateTime orderedAt
    ) {
        this.store = store;
        this.device = device;
        this.createdByUser = createdByUser;
        this.orderNumber = orderNumber;
        this.status = status;
        this.paymentStatus = paymentStatus;
        this.itemCount = itemCount;
        this.subtotalAmount = subtotalAmount;
        this.discountType = discountType;
        this.discountValue = discountValue;
        this.discountAmount = discountAmount;
        this.totalAmount = totalAmount;
        this.paidAmount = paidAmount;
        this.changeAmount = changeAmount;
        this.refundedAmount = refundedAmount;
        this.cogsAmount = BigDecimal.ZERO.setScale(2);
        this.refundedCogsAmount = BigDecimal.ZERO.setScale(2);
        this.netCogsAmount = BigDecimal.ZERO.setScale(2);
        this.grossProfitAmount = totalAmount.setScale(2);
        this.note = note;
        this.discountNote = discountNote;
        this.orderedAt = orderedAt;
        this.inventoryCommitted = false;
    }

    public void addItem(OrderItemEntity item) {
        items.add(item);
    }

    public void addPayment(PaymentEntity payment) {
        payments.add(payment);
    }

    public void addRefund(RefundEntity refund) {
        refunds.add(refund);
    }

    public void applyPayment(BigDecimal paidAmount, BigDecimal changeAmount, OffsetDateTime closedAt, boolean fullyPaid) {
        this.paidAmount = paidAmount;
        this.changeAmount = changeAmount;
        this.paymentStatus = fullyPaid ? "PAID" : "PARTIALLY_PAID";
        if (fullyPaid) {
            this.status = "PAID";
            this.closedAt = closedAt;
        }
    }

    public void applyRefund(BigDecimal refundedAmount, OffsetDateTime refundedAt) {
        this.refundedAmount = refundedAmount;
        recalculateProfitMetrics();
        if (refundedAmount.compareTo(this.paidAmount) >= 0) {
            this.status = "REFUNDED";
            this.paymentStatus = "REFUNDED";
            this.closedAt = refundedAt;
        } else {
            this.status = "PARTIALLY_REFUNDED";
            this.paymentStatus = "PARTIALLY_REFUNDED";
        }
    }

    public void voidOrder(OffsetDateTime voidedAt, String voidNote) {
        this.status = "VOIDED";
        this.paymentStatus = "VOIDED";
        this.voidedAt = voidedAt;
        this.voidNote = voidNote;
        this.closedAt = voidedAt;
    }

    public void markInventoryCommitted() {
        this.inventoryCommitted = true;
    }

    public void applyCostOfGoods(BigDecimal cogsAmount) {
        this.cogsAmount = cogsAmount;
        recalculateProfitMetrics();
    }

    public void applyRefundedCostOfGoods(BigDecimal refundedCogsAmount) {
        this.refundedCogsAmount = refundedCogsAmount;
        recalculateProfitMetrics();
    }

    public StoreEntity getStore() {
        return store;
    }

    public DeviceEntity getDevice() {
        return device;
    }

    public UserEntity getCreatedByUser() {
        return createdByUser;
    }

    public String getOrderNumber() {
        return orderNumber;
    }

    public String getStatus() {
        return status;
    }

    public String getPaymentStatus() {
        return paymentStatus;
    }

    public int getItemCount() {
        return itemCount;
    }

    public BigDecimal getSubtotalAmount() {
        return subtotalAmount;
    }

    public String getDiscountType() {
        return discountType;
    }

    public BigDecimal getDiscountValue() {
        return discountValue;
    }

    public BigDecimal getDiscountAmount() {
        return discountAmount;
    }

    public BigDecimal getTotalAmount() {
        return totalAmount;
    }

    public BigDecimal getPaidAmount() {
        return paidAmount;
    }

    public BigDecimal getChangeAmount() {
        return changeAmount;
    }

    public BigDecimal getRefundedAmount() {
        return refundedAmount;
    }

    public BigDecimal getCogsAmount() {
        return cogsAmount;
    }

    public BigDecimal getRefundedCogsAmount() {
        return refundedCogsAmount;
    }

    public BigDecimal getNetCogsAmount() {
        return netCogsAmount;
    }

    public BigDecimal getGrossProfitAmount() {
        return grossProfitAmount;
    }

    public String getNote() {
        return note;
    }

    public String getDiscountNote() {
        return discountNote;
    }

    public OffsetDateTime getOrderedAt() {
        return orderedAt;
    }

    public OffsetDateTime getClosedAt() {
        return closedAt;
    }

    public OffsetDateTime getVoidedAt() {
        return voidedAt;
    }

    public String getVoidNote() {
        return voidNote;
    }

    public boolean isInventoryCommitted() {
        return inventoryCommitted;
    }

    public List<OrderItemEntity> getItems() {
        return items;
    }

    public List<PaymentEntity> getPayments() {
        return payments;
    }

    public List<RefundEntity> getRefunds() {
        return refunds;
    }

    public ReceiptRedemptionEntity getReceiptRedemption() {
        return receiptRedemption;
    }

    public void setReceiptRedemption(ReceiptRedemptionEntity receiptRedemption) {
        this.receiptRedemption = receiptRedemption;
    }

    private void recalculateProfitMetrics() {
        this.netCogsAmount = cogsAmount.subtract(refundedCogsAmount).setScale(2, RoundingMode.HALF_UP);
        BigDecimal netSalesAmount = totalAmount.subtract(refundedAmount).setScale(2, RoundingMode.HALF_UP);
        this.grossProfitAmount = netSalesAmount.subtract(netCogsAmount).setScale(2, RoundingMode.HALF_UP);
    }
}
