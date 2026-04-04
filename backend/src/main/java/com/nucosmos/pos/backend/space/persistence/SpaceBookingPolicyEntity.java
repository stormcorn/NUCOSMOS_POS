package com.nucosmos.pos.backend.space.persistence;

import com.nucosmos.pos.backend.common.persistence.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.LocalTime;

@Entity
@Table(name = "space_booking_policies")
public class SpaceBookingPolicyEntity extends BaseEntity {

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "space_resource_id", nullable = false, unique = true)
    private SpaceResourceEntity spaceResource;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal hourlyRate;

    @Column(nullable = false, length = 10)
    private String currencyCode;

    @Column(nullable = false)
    private int minimumHours;

    @Column(nullable = false)
    private int bookingIntervalMinutes;

    @Column(nullable = false)
    private int bufferBeforeMinutes;

    @Column(nullable = false)
    private int bufferAfterMinutes;

    @Column(nullable = false)
    private LocalTime defaultOpenTime;

    @Column(nullable = false)
    private LocalTime defaultCloseTime;

    @Column(nullable = false)
    private int maxAttendees;

    @Column(nullable = false, length = 30)
    private String depositType;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal depositValue;

    @Column(columnDefinition = "text")
    private String cancellationPolicyText;

    @Column(columnDefinition = "text")
    private String houseRulesText;

    @Column(nullable = false)
    private boolean active;

    protected SpaceBookingPolicyEntity() {
    }

    public SpaceResourceEntity getSpaceResource() {
        return spaceResource;
    }

    public BigDecimal getHourlyRate() {
        return hourlyRate;
    }

    public String getCurrencyCode() {
        return currencyCode;
    }

    public int getMinimumHours() {
        return minimumHours;
    }

    public int getBookingIntervalMinutes() {
        return bookingIntervalMinutes;
    }

    public int getBufferBeforeMinutes() {
        return bufferBeforeMinutes;
    }

    public int getBufferAfterMinutes() {
        return bufferAfterMinutes;
    }

    public LocalTime getDefaultOpenTime() {
        return defaultOpenTime;
    }

    public LocalTime getDefaultCloseTime() {
        return defaultCloseTime;
    }

    public int getMaxAttendees() {
        return maxAttendees;
    }

    public String getDepositType() {
        return depositType;
    }

    public BigDecimal getDepositValue() {
        return depositValue;
    }

    public String getCancellationPolicyText() {
        return cancellationPolicyText;
    }

    public String getHouseRulesText() {
        return houseRulesText;
    }

    public boolean isActive() {
        return active;
    }
}
