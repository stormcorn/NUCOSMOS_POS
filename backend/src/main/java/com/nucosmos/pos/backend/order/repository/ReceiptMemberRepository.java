package com.nucosmos.pos.backend.order.repository;

import com.nucosmos.pos.backend.order.persistence.ReceiptMemberEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface ReceiptMemberRepository extends JpaRepository<ReceiptMemberEntity, UUID> {

    Optional<ReceiptMemberEntity> findByPhoneNumber(String phoneNumber);
}
