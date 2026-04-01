package com.nucosmos.pos.backend.order.repository;

import com.nucosmos.pos.backend.order.persistence.ReceiptCouponEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface ReceiptCouponRepository extends JpaRepository<ReceiptCouponEntity, UUID> {

    boolean existsByCouponCode(String couponCode);

    Optional<ReceiptCouponEntity> findBySourceRedemption_Id(UUID sourceRedemptionId);
}
