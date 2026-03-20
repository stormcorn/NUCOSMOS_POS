package com.nucosmos.pos.backend.order.repository;

import com.nucosmos.pos.backend.order.persistence.RefundEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface RefundRepository extends JpaRepository<RefundEntity, UUID> {
}
