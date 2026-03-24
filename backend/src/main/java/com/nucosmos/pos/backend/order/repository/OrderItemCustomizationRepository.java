package com.nucosmos.pos.backend.order.repository;

import com.nucosmos.pos.backend.order.persistence.OrderItemCustomizationEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

public interface OrderItemCustomizationRepository extends JpaRepository<OrderItemCustomizationEntity, UUID> {
    List<OrderItemCustomizationEntity> findAllByOrderItem_IdInOrderByLineNumberAscCreatedAtAsc(Collection<UUID> orderItemIds);
}
