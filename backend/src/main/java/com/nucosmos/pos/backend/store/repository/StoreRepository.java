package com.nucosmos.pos.backend.store.repository;

import com.nucosmos.pos.backend.store.persistence.StoreEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface StoreRepository extends JpaRepository<StoreEntity, UUID> {

    Optional<StoreEntity> findByCodeAndStatus(String code, String status);
}
