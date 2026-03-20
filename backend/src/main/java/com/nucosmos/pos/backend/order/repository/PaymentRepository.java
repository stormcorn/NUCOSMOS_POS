package com.nucosmos.pos.backend.order.repository;

import com.nucosmos.pos.backend.order.persistence.PaymentEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface PaymentRepository extends JpaRepository<PaymentEntity, UUID> {
}
