package com.nucosmos.pos.backend.order.repository;

import com.nucosmos.pos.backend.order.persistence.OrderEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.lang.Nullable;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.time.OffsetDateTime;

public interface OrderRepository extends JpaRepository<OrderEntity, UUID>, JpaSpecificationExecutor<OrderEntity> {

    @Override
    @EntityGraph(attributePaths = {"store", "device", "createdByUser", "items", "items.product", "payments", "payments.createdByUser", "refunds", "refunds.createdByUser", "refunds.payment"})
    Optional<OrderEntity> findById(UUID id);

    @Override
    @EntityGraph(attributePaths = {"store", "device", "createdByUser"})
    Page<OrderEntity> findAll(@Nullable Specification<OrderEntity> spec, Pageable pageable);

    @EntityGraph(attributePaths = {"store", "device", "createdByUser"})
    List<OrderEntity> findTop100ByStore_CodeOrderByOrderedAtDesc(String storeCode);

    List<OrderEntity> findAllByStore_CodeAndOrderedAtBetweenOrderByOrderedAtAsc(
            String storeCode,
            OffsetDateTime from,
            OffsetDateTime to
    );

    List<OrderEntity> findAllByStore_CodeAndDevice_IdAndOrderedAtBetweenOrderByOrderedAtAsc(
            String storeCode,
            UUID deviceId,
            OffsetDateTime from,
            OffsetDateTime to
    );
}
