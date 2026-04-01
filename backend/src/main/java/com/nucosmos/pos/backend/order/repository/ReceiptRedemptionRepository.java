package com.nucosmos.pos.backend.order.repository;

import com.nucosmos.pos.backend.order.persistence.ReceiptRedemptionEntity;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface ReceiptRedemptionRepository extends JpaRepository<ReceiptRedemptionEntity, UUID> {

    @EntityGraph(attributePaths = {"order", "order.store"})
    Optional<ReceiptRedemptionEntity> findByPublicToken(String publicToken);

    @EntityGraph(attributePaths = {"order", "order.store"})
    Optional<ReceiptRedemptionEntity> findByClaimCodeIgnoreCase(String claimCode);

    Optional<ReceiptRedemptionEntity> findByOrder_Id(UUID orderId);

    boolean existsByPublicToken(String publicToken);

    boolean existsByClaimCodeIgnoreCase(String claimCode);
}
