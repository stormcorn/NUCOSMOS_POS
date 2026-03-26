package com.nucosmos.pos.backend.auth.persistence;

import com.nucosmos.pos.backend.common.persistence.BaseEntity;
import com.nucosmos.pos.backend.store.persistence.StoreEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import java.time.OffsetDateTime;

@Entity
@Table(name = "phone_registration_requests")
public class PhoneRegistrationRequestEntity extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "store_id", nullable = false)
    private StoreEntity store;

    @Column(nullable = false, length = 30)
    private String phoneNumber;

    @Column(nullable = false, length = 255)
    private String pinHash;

    @Column(nullable = false, length = 40)
    private String status;

    @Column(nullable = false, length = 40)
    private String provider;

    @Column(length = 255)
    private String verificationSessionId;

    @Column(length = 255)
    private String firebaseUid;

    @Column
    private OffsetDateTime verificationCompletedAt;

    @Column(nullable = false)
    private OffsetDateTime expiresAt;

    public StoreEntity getStore() {
        return store;
    }

    public String getPhoneNumber() {
        return phoneNumber;
    }

    public String getPinHash() {
        return pinHash;
    }

    public String getStatus() {
        return status;
    }

    public String getProvider() {
        return provider;
    }

    public String getVerificationSessionId() {
        return verificationSessionId;
    }

    public String getFirebaseUid() {
        return firebaseUid;
    }

    public OffsetDateTime getVerificationCompletedAt() {
        return verificationCompletedAt;
    }

    public OffsetDateTime getExpiresAt() {
        return expiresAt;
    }

    public void setStore(StoreEntity store) {
        this.store = store;
    }

    public void setPhoneNumber(String phoneNumber) {
        this.phoneNumber = phoneNumber;
    }

    public void setPinHash(String pinHash) {
        this.pinHash = pinHash;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public void setProvider(String provider) {
        this.provider = provider;
    }

    public void setVerificationSessionId(String verificationSessionId) {
        this.verificationSessionId = verificationSessionId;
    }

    public void setFirebaseUid(String firebaseUid) {
        this.firebaseUid = firebaseUid;
    }

    public void setVerificationCompletedAt(OffsetDateTime verificationCompletedAt) {
        this.verificationCompletedAt = verificationCompletedAt;
    }

    public void setExpiresAt(OffsetDateTime expiresAt) {
        this.expiresAt = expiresAt;
    }
}
