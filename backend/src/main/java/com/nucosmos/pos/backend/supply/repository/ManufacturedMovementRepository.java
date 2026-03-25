package com.nucosmos.pos.backend.supply.repository;

import com.nucosmos.pos.backend.supply.persistence.ManufacturedMovementEntity;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

public interface ManufacturedMovementRepository extends JpaRepository<ManufacturedMovementEntity, UUID> {

    @EntityGraph(attributePaths = {"manufacturedItem"})
    List<ManufacturedMovementEntity> findTop100ByManufacturedItem_Store_CodeOrderByOccurredAtDescCreatedAtDesc(String storeCode);

    @EntityGraph(attributePaths = {"manufacturedItem"})
    List<ManufacturedMovementEntity> findAllByManufacturedItem_Store_CodeAndOccurredAtBetweenOrderByOccurredAtAsc(
            String storeCode,
            OffsetDateTime from,
            OffsetDateTime to
    );
}
