package com.nucosmos.pos.backend.shift.repository;

import com.nucosmos.pos.backend.shift.persistence.ShiftEntity;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface ShiftRepository extends JpaRepository<ShiftEntity, UUID> {

    @Override
    @EntityGraph(attributePaths = {"store", "device", "openedByUser", "closedByUser"})
    Optional<ShiftEntity> findById(UUID id);

    @EntityGraph(attributePaths = {"store", "device", "openedByUser", "closedByUser"})
    Optional<ShiftEntity> findFirstByStore_CodeAndDevice_IdAndStatusOrderByOpenedAtDesc(
            String storeCode,
            UUID deviceId,
            String status
    );
}
