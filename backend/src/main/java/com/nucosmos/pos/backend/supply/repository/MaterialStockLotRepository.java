package com.nucosmos.pos.backend.supply.repository;

import com.nucosmos.pos.backend.supply.persistence.MaterialStockLotEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface MaterialStockLotRepository extends JpaRepository<MaterialStockLotEntity, UUID> {

    List<MaterialStockLotEntity> findAllByMaterial_Store_CodeOrderByExpiryDateAscReceivedAtAscCreatedAtAsc(String storeCode);

    List<MaterialStockLotEntity> findAllByMaterial_IdAndRemainingQuantityGreaterThanOrderByExpiryDateAscReceivedAtAscCreatedAtAsc(
            UUID materialId,
            int remainingQuantity
    );
}
