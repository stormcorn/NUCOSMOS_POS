package com.nucosmos.pos.backend.order.repository;

import com.nucosmos.pos.backend.order.persistence.ReceiptMemberSessionEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface ReceiptMemberSessionRepository extends JpaRepository<ReceiptMemberSessionEntity, UUID> {

    Optional<ReceiptMemberSessionEntity> findByPublicToken(String publicToken);

    boolean existsByPublicToken(String publicToken);
}
