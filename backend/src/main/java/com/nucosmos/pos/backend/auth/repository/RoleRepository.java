package com.nucosmos.pos.backend.auth.repository;

import com.nucosmos.pos.backend.auth.persistence.RoleEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface RoleRepository extends JpaRepository<RoleEntity, UUID> {

    Optional<RoleEntity> findByCodeAndActiveTrue(String code);

    @Query("""
            select role.code
            from UserRoleEntity userRole
            join userRole.role role
            where userRole.user.id = :userId
              and role.active = true
            order by role.code
            """)
    List<String> findActiveRoleCodesByUserId(UUID userId);
}
