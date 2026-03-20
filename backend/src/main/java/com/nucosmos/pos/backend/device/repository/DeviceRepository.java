package com.nucosmos.pos.backend.device.repository;

import com.nucosmos.pos.backend.device.persistence.DeviceEntity;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface DeviceRepository extends JpaRepository<DeviceEntity, UUID> {

    @EntityGraph(attributePaths = "store")
    Optional<DeviceEntity> findByStore_CodeAndDeviceCodeAndStatus(String storeCode, String deviceCode, String status);

    @EntityGraph(attributePaths = "store")
    List<DeviceEntity> findAllByOrderByStore_CodeAscNameAsc();

    @EntityGraph(attributePaths = "store")
    List<DeviceEntity> findAllByStore_CodeOrderByNameAsc(String storeCode);

    @EntityGraph(attributePaths = "store")
    List<DeviceEntity> findAllByStore_CodeAndStatusOrderByNameAsc(String storeCode, String status);

    @EntityGraph(attributePaths = "store")
    List<DeviceEntity> findAllByStatusOrderByStore_CodeAscNameAsc(String status);
}
