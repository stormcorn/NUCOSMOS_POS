package com.nucosmos.pos.backend.order.persistence;

import com.nucosmos.pos.backend.common.persistence.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import java.time.OffsetDateTime;

@Entity
@Table(name = "receipt_member_trusted_devices")
public class ReceiptMemberTrustedDeviceEntity extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "member_id", nullable = false)
    private ReceiptMemberEntity member;

    @Column(nullable = false, unique = true, length = 128)
    private String deviceToken;

    @Column(nullable = false, length = 255)
    private String deviceLabel;

    @Column(nullable = false)
    private OffsetDateTime expiresAt;

    @Column(nullable = false)
    private OffsetDateTime lastAuthenticatedAt;

    protected ReceiptMemberTrustedDeviceEntity() {
    }

    public ReceiptMemberTrustedDeviceEntity(
            ReceiptMemberEntity member,
            String deviceToken,
            String deviceLabel,
            OffsetDateTime expiresAt,
            OffsetDateTime lastAuthenticatedAt
    ) {
        this.member = member;
        this.deviceToken = deviceToken;
        this.deviceLabel = deviceLabel;
        this.expiresAt = expiresAt;
        this.lastAuthenticatedAt = lastAuthenticatedAt;
    }

    public ReceiptMemberEntity getMember() {
        return member;
    }

    public String getDeviceToken() {
        return deviceToken;
    }

    public String getDeviceLabel() {
        return deviceLabel;
    }

    public OffsetDateTime getExpiresAt() {
        return expiresAt;
    }

    public OffsetDateTime getLastAuthenticatedAt() {
        return lastAuthenticatedAt;
    }

    public boolean isExpired(OffsetDateTime now) {
        return expiresAt.isBefore(now);
    }

    public void refresh(OffsetDateTime authenticatedAt, OffsetDateTime nextExpiresAt) {
        this.lastAuthenticatedAt = authenticatedAt;
        this.expiresAt = nextExpiresAt;
    }

    public void relink(
            ReceiptMemberEntity member,
            String deviceLabel,
            OffsetDateTime authenticatedAt,
            OffsetDateTime nextExpiresAt
    ) {
        this.member = member;
        this.deviceLabel = deviceLabel;
        this.lastAuthenticatedAt = authenticatedAt;
        this.expiresAt = nextExpiresAt;
    }
}
