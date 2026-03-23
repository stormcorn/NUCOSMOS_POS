package com.nucosmos.pos.backend.supply.repository;

import com.nucosmos.pos.backend.supply.persistence.PurchaseOrderEntity;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface PurchaseOrderRepository extends JpaRepository<PurchaseOrderEntity, UUID> {

    @EntityGraph(attributePaths = {
            "store", "supplier", "createdByUser", "lines", "lines.materialItem", "lines.packagingItem"
    })
    List<PurchaseOrderEntity> findTop50ByStore_CodeOrderByCreatedAtDesc(String storeCode);

    @EntityGraph(attributePaths = {
            "store", "supplier", "createdByUser", "lines", "lines.materialItem", "lines.packagingItem"
    })
    Optional<PurchaseOrderEntity> findByIdAndStore_Code(UUID purchaseOrderId, String storeCode);
}
