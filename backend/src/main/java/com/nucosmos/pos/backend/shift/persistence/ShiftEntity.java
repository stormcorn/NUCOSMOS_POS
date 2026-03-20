package com.nucosmos.pos.backend.shift.persistence;

import com.nucosmos.pos.backend.auth.persistence.UserEntity;
import com.nucosmos.pos.backend.common.persistence.BaseEntity;
import com.nucosmos.pos.backend.device.persistence.DeviceEntity;
import com.nucosmos.pos.backend.store.persistence.StoreEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

@Entity
@Table(name = "shifts")
public class ShiftEntity extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "store_id", nullable = false)
    private StoreEntity store;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "device_id", nullable = false)
    private DeviceEntity device;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "opened_by_user_id", nullable = false)
    private UserEntity openedByUser;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "closed_by_user_id")
    private UserEntity closedByUser;

    @Column(nullable = false, length = 20)
    private String status;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal openingCashAmount;

    @Column(precision = 10, scale = 2)
    private BigDecimal closingCashAmount;

    @Column(precision = 10, scale = 2)
    private BigDecimal expectedCashAmount;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal cashSalesAmount;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal cardSalesAmount;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal refundedAmount;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal netSalesAmount;

    @Column(nullable = false)
    private int orderCount;

    @Column(nullable = false)
    private int voidedOrderCount;

    @Column(length = 255)
    private String note;

    @Column(length = 255)
    private String closeNote;

    @Column(nullable = false)
    private OffsetDateTime openedAt;

    @Column
    private OffsetDateTime closedAt;

    protected ShiftEntity() {
    }

    public ShiftEntity(
            StoreEntity store,
            DeviceEntity device,
            UserEntity openedByUser,
            BigDecimal openingCashAmount,
            String note,
            OffsetDateTime openedAt
    ) {
        this.store = store;
        this.device = device;
        this.openedByUser = openedByUser;
        this.status = "OPEN";
        this.openingCashAmount = openingCashAmount;
        this.cashSalesAmount = BigDecimal.ZERO;
        this.cardSalesAmount = BigDecimal.ZERO;
        this.refundedAmount = BigDecimal.ZERO;
        this.netSalesAmount = BigDecimal.ZERO;
        this.orderCount = 0;
        this.voidedOrderCount = 0;
        this.note = note;
        this.openedAt = openedAt;
    }

    public void close(
            UserEntity closedByUser,
            BigDecimal closingCashAmount,
            BigDecimal expectedCashAmount,
            BigDecimal cashSalesAmount,
            BigDecimal cardSalesAmount,
            BigDecimal refundedAmount,
            BigDecimal netSalesAmount,
            int orderCount,
            int voidedOrderCount,
            String closeNote,
            OffsetDateTime closedAt
    ) {
        this.closedByUser = closedByUser;
        this.status = "CLOSED";
        this.closingCashAmount = closingCashAmount;
        this.expectedCashAmount = expectedCashAmount;
        this.cashSalesAmount = cashSalesAmount;
        this.cardSalesAmount = cardSalesAmount;
        this.refundedAmount = refundedAmount;
        this.netSalesAmount = netSalesAmount;
        this.orderCount = orderCount;
        this.voidedOrderCount = voidedOrderCount;
        this.closeNote = closeNote;
        this.closedAt = closedAt;
    }

    public StoreEntity getStore() {
        return store;
    }

    public DeviceEntity getDevice() {
        return device;
    }

    public UserEntity getOpenedByUser() {
        return openedByUser;
    }

    public UserEntity getClosedByUser() {
        return closedByUser;
    }

    public String getStatus() {
        return status;
    }

    public BigDecimal getOpeningCashAmount() {
        return openingCashAmount;
    }

    public BigDecimal getClosingCashAmount() {
        return closingCashAmount;
    }

    public BigDecimal getExpectedCashAmount() {
        return expectedCashAmount;
    }

    public BigDecimal getCashSalesAmount() {
        return cashSalesAmount;
    }

    public BigDecimal getCardSalesAmount() {
        return cardSalesAmount;
    }

    public BigDecimal getRefundedAmount() {
        return refundedAmount;
    }

    public BigDecimal getNetSalesAmount() {
        return netSalesAmount;
    }

    public int getOrderCount() {
        return orderCount;
    }

    public int getVoidedOrderCount() {
        return voidedOrderCount;
    }

    public String getNote() {
        return note;
    }

    public String getCloseNote() {
        return closeNote;
    }

    public OffsetDateTime getOpenedAt() {
        return openedAt;
    }

    public OffsetDateTime getClosedAt() {
        return closedAt;
    }
}
