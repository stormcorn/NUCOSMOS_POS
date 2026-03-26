package com.nucosmos.pos.backend.auth.repository;

import com.nucosmos.pos.backend.auth.persistence.PhoneRegistrationRequestEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface PhoneRegistrationRequestRepository extends JpaRepository<PhoneRegistrationRequestEntity, UUID> {

    @Query("""
            select request
            from PhoneRegistrationRequestEntity request
            join fetch request.store store
            where request.id = :registrationId
            """)
    Optional<PhoneRegistrationRequestEntity> findDetailedById(UUID registrationId);

    boolean existsByPhoneNumberAndStatusIn(String phoneNumber, List<String> statuses);
}
