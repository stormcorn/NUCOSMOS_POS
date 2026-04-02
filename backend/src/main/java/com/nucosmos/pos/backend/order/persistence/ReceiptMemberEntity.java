package com.nucosmos.pos.backend.order.persistence;

import com.nucosmos.pos.backend.common.persistence.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

import java.time.OffsetDateTime;

@Entity
@Table(name = "receipt_members")
public class ReceiptMemberEntity extends BaseEntity {

    @Column(nullable = false, length = 80)
    private String displayName;

    @Column(nullable = false, unique = true, length = 30)
    private String phoneNumber;

    @Column(unique = true, length = 128)
    private String firebaseUid;

    @Column(nullable = false, length = 20)
    private String status;

    @Column
    private OffsetDateTime lastClaimedAt;

    @Column(nullable = false)
    private int pointBalance;

    @Column(nullable = false)
    private int totalClaims;

    protected ReceiptMemberEntity() {
    }

    public ReceiptMemberEntity(String displayName, String phoneNumber, String status) {
        this.displayName = displayName;
        this.phoneNumber = phoneNumber;
        this.status = status;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getPhoneNumber() {
        return phoneNumber;
    }

    public String getStatus() {
        return status;
    }

    public String getFirebaseUid() {
        return firebaseUid;
    }

    public OffsetDateTime getLastClaimedAt() {
        return lastClaimedAt;
    }

    public int getPointBalance() {
        return pointBalance;
    }

    public int getTotalClaims() {
        return totalClaims;
    }

    public void updateProfile(String displayName, String phoneNumber) {
        this.displayName = displayName;
        this.phoneNumber = phoneNumber;
    }

    public void bindFirebaseUid(String firebaseUid) {
        this.firebaseUid = firebaseUid;
    }

    public void markClaimed(OffsetDateTime claimedAt, int awardedPoints) {
        this.lastClaimedAt = claimedAt;
        this.pointBalance += Math.max(awardedPoints, 0);
        this.totalClaims += 1;
    }
}
