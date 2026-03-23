package com.nucosmos.pos.backend.supply.repository;

import com.nucosmos.pos.backend.supply.persistence.PackagingMovementEntity;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface PackagingMovementRepository extends JpaRepository<PackagingMovementEntity, UUID> {

    @EntityGraph(attributePaths = {"packagingItem"})
    List<PackagingMovementEntity> findTop100ByPackagingItem_Store_CodeOrderByOccurredAtDescCreatedAtDesc(String storeCode);
}
