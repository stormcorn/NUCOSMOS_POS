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
@Table(name = "receipt_member_sessions")
public class ReceiptMemberSessionEntity extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "member_id", nullable = false)
    private ReceiptMemberEntity member;

    @Column(nullable = false, unique = true, length = 96)
    private String publicToken;

    @Column(nullable = false)
    private OffsetDateTime expiresAt;

    @Column(nullable = false)
    private OffsetDateTime lastAuthenticatedAt;

    protected ReceiptMemberSessionEntity() {
    }

    public ReceiptMemberSessionEntity(
            ReceiptMemberEntity member,
            String publicToken,
            OffsetDateTime expiresAt,
            OffsetDateTime lastAuthenticatedAt
    ) {
        this.member = member;
        this.publicToken = publicToken;
        this.expiresAt = expiresAt;
        this.lastAuthenticatedAt = lastAuthenticatedAt;
    }

    public ReceiptMemberEntity getMember() {
        return member;
    }

    public String getPublicToken() {
        return publicToken;
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
}
