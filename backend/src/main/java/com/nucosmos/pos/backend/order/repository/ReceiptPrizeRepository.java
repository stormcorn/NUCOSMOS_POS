package com.nucosmos.pos.backend.order.repository;

import com.nucosmos.pos.backend.order.persistence.ReceiptPrizeEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface ReceiptPrizeRepository extends JpaRepository<ReceiptPrizeEntity, UUID> {

    List<ReceiptPrizeEntity> findAllByOrderByDisplayOrderAscCreatedAtAsc();

    List<ReceiptPrizeEntity> findByActiveTrueOrderByDisplayOrderAscCreatedAtAsc();
}
