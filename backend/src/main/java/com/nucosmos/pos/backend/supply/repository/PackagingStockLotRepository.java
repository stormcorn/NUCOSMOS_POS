package com.nucosmos.pos.backend.supply.repository;

import com.nucosmos.pos.backend.supply.persistence.PackagingStockLotEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface PackagingStockLotRepository extends JpaRepository<PackagingStockLotEntity, UUID> {

    List<PackagingStockLotEntity> findAllByPackagingItem_Store_CodeOrderByExpiryDateAscReceivedAtAscCreatedAtAsc(String storeCode);

    List<PackagingStockLotEntity> findAllByPackagingItem_IdAndRemainingQuantityGreaterThanOrderByExpiryDateAscReceivedAtAscCreatedAtAsc(
            UUID packagingItemId,
            int remainingQuantity
    );
}
