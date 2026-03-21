package com.nucosmos.pos.backend.inventory.repository;

import com.nucosmos.pos.backend.inventory.persistence.InventoryStockEntity;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface InventoryStockRepository extends JpaRepository<InventoryStockEntity, UUID> {

    @EntityGraph(attributePaths = {"product", "product.category"})
    List<InventoryStockEntity> findAllByStore_IdOrderByProduct_Category_DisplayOrderAscProduct_NameAsc(UUID storeId);

    @EntityGraph(attributePaths = {"product", "product.category"})
    Optional<InventoryStockEntity> findByStore_IdAndProduct_Id(UUID storeId, UUID productId);
}
