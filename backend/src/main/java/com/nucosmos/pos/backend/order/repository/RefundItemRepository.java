package com.nucosmos.pos.backend.order.repository;

import com.nucosmos.pos.backend.order.persistence.RefundItemEntity;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface RefundItemRepository extends JpaRepository<RefundItemEntity, UUID> {

    @EntityGraph(attributePaths = {"orderItem", "product"})
    List<RefundItemEntity> findAllByRefund_Order_Id(UUID orderId);

    @EntityGraph(attributePaths = {"orderItem", "product"})
    List<RefundItemEntity> findAllByRefund_Id(UUID refundId);
}
