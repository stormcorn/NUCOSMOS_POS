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

    @Column(nullable = false, length = 20)
    private String status;

    @Column
    private OffsetDateTime lastLoginAt;

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

    public OffsetDateTime getLastLoginAt() {
        return lastLoginAt;
    }

    public void markLoggedIn() {
        this.lastLoginAt = OffsetDateTime.now();
    }
}
