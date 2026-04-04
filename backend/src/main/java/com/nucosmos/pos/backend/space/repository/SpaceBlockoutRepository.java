package com.nucosmos.pos.backend.space.repository;

import com.nucosmos.pos.backend.space.persistence.SpaceBlockoutEntity;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface SpaceBlockoutRepository extends JpaRepository<SpaceBlockoutEntity, UUID> {

    @EntityGraph(attributePaths = {"spaceResource", "spaceResource.store", "createdByUser"})
    List<SpaceBlockoutEntity> findAllBySpaceResource_IdAndEndAtAfterAndStartAtBeforeOrderByStartAtAsc(
            UUID spaceResourceId,
            OffsetDateTime from,
            OffsetDateTime to
    );

    @EntityGraph(attributePaths = {"spaceResource", "spaceResource.store", "createdByUser"})
    List<SpaceBlockoutEntity> findAllBySpaceResource_Store_CodeAndEndAtAfterAndStartAtBeforeOrderByStartAtAsc(
            String storeCode,
            OffsetDateTime from,
            OffsetDateTime to
    );

    @EntityGraph(attributePaths = {"spaceResource", "spaceResource.store", "createdByUser"})
    Optional<SpaceBlockoutEntity> findByIdAndSpaceResource_Store_Code(UUID id, String storeCode);
}
