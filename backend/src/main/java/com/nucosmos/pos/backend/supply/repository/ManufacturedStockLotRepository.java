package com.nucosmos.pos.backend.supply.repository;

import com.nucosmos.pos.backend.supply.persistence.ManufacturedStockLotEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface ManufacturedStockLotRepository extends JpaRepository<ManufacturedStockLotEntity, UUID> {

    List<ManufacturedStockLotEntity> findAllByManufacturedItem_Store_CodeOrderByExpiryDateAscReceivedAtAscCreatedAtAsc(String storeCode);

    List<ManufacturedStockLotEntity> findAllByManufacturedItem_IdAndRemainingQuantityGreaterThanOrderByExpiryDateAscReceivedAtAscCreatedAtAsc(
            UUID manufacturedItemId,
            int remainingQuantity
    );
}
