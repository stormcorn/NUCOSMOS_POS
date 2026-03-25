package com.nucosmos.pos.backend.store.repository;

import com.nucosmos.pos.backend.store.persistence.StoreEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface StoreRepository extends JpaRepository<StoreEntity, UUID> {

    Optional<StoreEntity> findByCodeAndStatus(String code, String status);

    List<StoreEntity> findAllByCodeIn(List<String> codes);

    List<StoreEntity> findAllByStatusOrderByCodeAsc(String status);

    List<StoreEntity> findAllByOrderByCodeAsc();
}
