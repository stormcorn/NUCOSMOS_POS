package com.nucosmos.pos.backend.supply.repository;

import com.nucosmos.pos.backend.supply.persistence.SupplierEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface SupplierRepository extends JpaRepository<SupplierEntity, UUID> {

    List<SupplierEntity> findAllByStore_CodeOrderByActiveDescNameAsc(String storeCode);

    Optional<SupplierEntity> findByIdAndStore_Code(UUID supplierId, String storeCode);

    boolean existsByStore_CodeAndCodeIgnoreCase(String storeCode, String code);

    boolean existsByStore_CodeAndCodeIgnoreCaseAndIdNot(String storeCode, String code, UUID excludeId);
}
