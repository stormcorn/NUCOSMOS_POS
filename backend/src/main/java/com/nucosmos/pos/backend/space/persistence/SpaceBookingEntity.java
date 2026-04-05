package com.nucosmos.pos.backend.space.persistence;

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
@Table(name = "space_bookings")
public class SpaceBookingEntity extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "space_resource_id", nullable = false)
    private SpaceResourceEntity spaceResource;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "approved_by_user_id")
    private UserEntity approvedByUser;

    @Column(nullable = false, unique = true, length = 60)
    private String bookingNumber;

    @Column(nullable = false, length = 30)
    private String status;

    @Column(nullable = false, length = 30)
    private String source;

    @Column(nullable = false, length = 120)
    private String customerName;

    @Column(nullable = false, length = 40)
    private String customerPhone;

    @Column(length = 160)
    private String customerEmail;

    @Column(length = 240)
    private String purpose;

    @Column(length = 500)
    private String eventLink;

    @Column(nullable = false)
    private int attendeeCount;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal subtotalAmount;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal depositAmount;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal paidAmount;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal balanceAmount;

    @Column(columnDefinition = "text")
    private String note;

    @Column(columnDefinition = "text")
    private String internalNote;

    @Column(nullable = false)
    private OffsetDateTime startAt;

    @Column(nullable = false)
    private OffsetDateTime endAt;

    @Column
    private OffsetDateTime approvedAt;

    @Column
    private OffsetDateTime cancelledAt;

    @Column
    private OffsetDateTime completedAt;

    protected SpaceBookingEntity() {
    }

    public static SpaceBookingEntity create(
            SpaceResourceEntity spaceResource,
            String bookingNumber,
            String status,
            String source,
            String customerName,
            String customerPhone,
            String customerEmail,
            String purpose,
            String eventLink,
            int attendeeCount,
            BigDecimal subtotalAmount,
            BigDecimal depositAmount,
            BigDecimal paidAmount,
            BigDecimal balanceAmount,
            String note,
            String internalNote,
            OffsetDateTime startAt,
            OffsetDateTime endAt
    ) {
        SpaceBookingEntity entity = new SpaceBookingEntity();
        entity.spaceResource = spaceResource;
        entity.bookingNumber = bookingNumber;
        entity.status = status;
        entity.source = source;
        entity.customerName = customerName;
        entity.customerPhone = customerPhone;
        entity.customerEmail = customerEmail;
        entity.purpose = purpose;
        entity.eventLink = eventLink;
        entity.attendeeCount = attendeeCount;
        entity.subtotalAmount = subtotalAmount;
        entity.depositAmount = depositAmount;
        entity.paidAmount = paidAmount;
        entity.balanceAmount = balanceAmount;
        entity.note = note;
        entity.internalNote = internalNote;
        entity.startAt = startAt;
        entity.endAt = endAt;
        return entity;
    }

    public SpaceResourceEntity getSpaceResource() {
        return spaceResource;
    }

    public UserEntity getApprovedByUser() {
        return approvedByUser;
    }

    public String getBookingNumber() {
        return bookingNumber;
    }

    public String getStatus() {
        return status;
    }

    public String getSource() {
        return source;
    }

    public String getCustomerName() {
        return customerName;
    }

    public String getCustomerPhone() {
        return customerPhone;
    }

    public String getCustomerEmail() {
        return customerEmail;
    }

    public String getPurpose() {
        return purpose;
    }

    public String getEventLink() {
        return eventLink;
    }

    public int getAttendeeCount() {
        return attendeeCount;
    }

    public BigDecimal getSubtotalAmount() {
        return subtotalAmount;
    }

    public BigDecimal getDepositAmount() {
        return depositAmount;
    }

    public BigDecimal getPaidAmount() {
        return paidAmount;
    }

    public BigDecimal getBalanceAmount() {
        return balanceAmount;
    }

    public String getNote() {
        return note;
    }

    public String getInternalNote() {
        return internalNote;
    }

    public OffsetDateTime getStartAt() {
        return startAt;
    }

    public OffsetDateTime getEndAt() {
        return endAt;
    }

    public OffsetDateTime getApprovedAt() {
        return approvedAt;
    }

    public OffsetDateTime getCancelledAt() {
        return cancelledAt;
    }

    public OffsetDateTime getCompletedAt() {
        return completedAt;
    }

    public void approve(UserEntity approvedByUser, String internalNote) {
        this.status = "CONFIRMED";
        this.approvedByUser = approvedByUser;
        this.approvedAt = OffsetDateTime.now();
        this.internalNote = internalNote;
    }

    public void reject(String internalNote) {
        this.status = "REJECTED";
        this.internalNote = internalNote;
    }

    public void cancel(String internalNote) {
        this.status = "CANCELLED";
        this.cancelledAt = OffsetDateTime.now();
        this.internalNote = internalNote;
    }
}
