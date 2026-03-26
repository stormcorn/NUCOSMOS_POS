package com.nucosmos.pos.backend.auth.persistence;

import com.nucosmos.pos.backend.common.persistence.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

import java.time.OffsetDateTime;

@Entity
@Table(name = "users")
public class UserEntity extends BaseEntity {

    @Column(nullable = false, unique = true, length = 50)
    private String employeeCode;

    @Column(nullable = false, length = 120)
    private String displayName;

    @Column(nullable = false, length = 255)
    private String pinHash;

    @Column(unique = true, length = 30)
    private String phoneNumber;

    @Column(nullable = false, length = 20)
    private String status;

    @Column
    private OffsetDateTime lastLoginAt;

    @Column
    private OffsetDateTime phoneVerifiedAt;

    public String getEmployeeCode() {
        return employeeCode;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getPinHash() {
        return pinHash;
    }

    public String getStatus() {
        return status;
    }

    public String getPhoneNumber() {
        return phoneNumber;
    }

    public OffsetDateTime getLastLoginAt() {
        return lastLoginAt;
    }

    public OffsetDateTime getPhoneVerifiedAt() {
        return phoneVerifiedAt;
    }

    public void setEmployeeCode(String employeeCode) {
        this.employeeCode = employeeCode;
    }

    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }

    public void setPinHash(String pinHash) {
        this.pinHash = pinHash;
    }

    public void setPhoneNumber(String phoneNumber) {
        this.phoneNumber = phoneNumber;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public void setPhoneVerifiedAt(OffsetDateTime phoneVerifiedAt) {
        this.phoneVerifiedAt = phoneVerifiedAt;
    }

    public void markLoggedIn() {
        this.lastLoginAt = OffsetDateTime.now();
    }
}
