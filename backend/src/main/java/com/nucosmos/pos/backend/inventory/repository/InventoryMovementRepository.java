package com.nucosmos.pos.backend.inventory.repository;

import com.nucosmos.pos.backend.inventory.persistence.InventoryMovementEntity;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface InventoryMovementRepository extends JpaRepository<InventoryMovementEntity, UUID> {

    @EntityGraph(attributePaths = {"product", "product.category"})
    List<InventoryMovementEntity> findTop100ByStore_IdOrderByOccurredAtDescCreatedAtDesc(UUID storeId);
}
