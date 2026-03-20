package com.nucosmos.pos.backend.device.repository;

import com.nucosmos.pos.backend.device.persistence.DeviceEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface DeviceRepository extends JpaRepository<DeviceEntity, UUID> {

    Optional<DeviceEntity> findByStore_CodeAndDeviceCodeAndStatus(String storeCode, String deviceCode, String status);
}
