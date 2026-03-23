package com.nucosmos.pos.backend.inventory.stocktake.repository;

import com.nucosmos.pos.backend.inventory.stocktake.persistence.InventoryStocktakeEntity;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface InventoryStocktakeRepository extends JpaRepository<InventoryStocktakeEntity, UUID> {

    @EntityGraph(attributePaths = {"store", "createdByUser", "items", "items.product", "items.product.category"})
    List<InventoryStocktakeEntity> findTop20ByStore_CodeOrderByCountedAtDesc(String storeCode);
}
