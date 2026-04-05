package com.nucosmos.pos.backend.space.repository;

import com.nucosmos.pos.backend.space.persistence.SpaceBookingEntity;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.OffsetDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface SpaceBookingRepository extends JpaRepository<SpaceBookingEntity, UUID> {

    @EntityGraph(attributePaths = {"spaceResource", "spaceResource.store", "approvedByUser"})
    List<SpaceBookingEntity> findAllBySpaceResource_IdAndEndAtAfterAndStartAtBeforeAndStatusInOrderByStartAtAsc(
            UUID spaceResourceId,
            OffsetDateTime from,
            OffsetDateTime to,
            Collection<String> statuses
    );

    @EntityGraph(attributePaths = {"spaceResource", "spaceResource.store", "approvedByUser"})
    List<SpaceBookingEntity> findAllBySpaceResource_Store_CodeAndEndAtAfterAndStartAtBeforeOrderByStartAtAsc(
            String storeCode,
            OffsetDateTime from,
            OffsetDateTime to
    );

    @EntityGraph(attributePaths = {"spaceResource", "spaceResource.store", "approvedByUser"})
    List<SpaceBookingEntity> findAllBySpaceResource_Store_CodeAndStatusAndEndAtAfterAndStartAtBeforeOrderByStartAtAsc(
            String storeCode,
            String status,
            OffsetDateTime from,
            OffsetDateTime to
    );

    @EntityGraph(attributePaths = {"spaceResource", "spaceResource.store", "approvedByUser"})
    Optional<SpaceBookingEntity> findByIdAndSpaceResource_Store_Code(UUID id, String storeCode);

    @EntityGraph(attributePaths = {"spaceResource", "spaceResource.store", "approvedByUser"})
    List<SpaceBookingEntity> findAllByStatusAndStartAtAfterOrderByStartAtAsc(
            String status,
            OffsetDateTime startAt
    );
}
