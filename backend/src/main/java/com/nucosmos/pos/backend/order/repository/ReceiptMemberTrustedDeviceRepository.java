package com.nucosmos.pos.backend.order.repository;

import com.nucosmos.pos.backend.order.persistence.ReceiptMemberTrustedDeviceEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface ReceiptMemberTrustedDeviceRepository extends JpaRepository<ReceiptMemberTrustedDeviceEntity, UUID> {

    Optional<ReceiptMemberTrustedDeviceEntity> findByDeviceToken(String deviceToken);

    boolean existsByDeviceToken(String deviceToken);

    void deleteByDeviceToken(String deviceToken);
}
