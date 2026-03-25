package com.nucosmos.pos.backend.supply.repository;

import com.nucosmos.pos.backend.supply.persistence.ManufacturedItemEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ManufacturedItemRepository extends JpaRepository<ManufacturedItemEntity, UUID> {

    List<ManufacturedItemEntity> findAllByStore_CodeOrderByActiveDescNameAsc(String storeCode);

    Optional<ManufacturedItemEntity> findByIdAndStore_Code(UUID id, String storeCode);

    boolean existsByStore_CodeAndSkuIgnoreCase(String storeCode, String sku);

    boolean existsByStore_CodeAndSkuIgnoreCaseAndIdNot(String storeCode, String sku, UUID excludeId);
}
