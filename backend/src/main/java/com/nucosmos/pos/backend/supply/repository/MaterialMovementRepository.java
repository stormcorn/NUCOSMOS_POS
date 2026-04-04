package com.nucosmos.pos.backend.supply.repository;

import com.nucosmos.pos.backend.supply.persistence.MaterialMovementEntity;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface MaterialMovementRepository extends JpaRepository<MaterialMovementEntity, UUID> {

    @EntityGraph(attributePaths = {"material"})
    List<MaterialMovementEntity> findTop100ByMaterial_Store_CodeOrderByOccurredAtDescCreatedAtDesc(String storeCode);

    @EntityGraph(attributePaths = {"material"})
    List<MaterialMovementEntity> findAllByMaterial_Store_CodeAndOccurredAtBetweenOrderByOccurredAtAsc(
            String storeCode,
            java.time.OffsetDateTime from,
            java.time.OffsetDateTime to
    );

    @EntityGraph(attributePaths = {"material"})
    List<MaterialMovementEntity> findAllByReferenceTypeAndReferenceIdOrderByOccurredAtAscCreatedAtAsc(
            String referenceType,
            UUID referenceId
    );
}
