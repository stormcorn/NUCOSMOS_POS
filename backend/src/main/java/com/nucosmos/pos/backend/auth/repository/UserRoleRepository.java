package com.nucosmos.pos.backend.auth.repository;

import com.nucosmos.pos.backend.auth.persistence.UserRoleEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.UUID;

public interface UserRoleRepository extends JpaRepository<UserRoleEntity, UUID> {

    @Query("""
            select userRole
            from UserRoleEntity userRole
            join fetch userRole.role role
            where userRole.user.id in :userIds
            order by role.code
            """)
    List<UserRoleEntity> findAllByUserIdIn(List<UUID> userIds);

    @Modifying
    @Query("delete from UserRoleEntity userRole where userRole.user.id = :userId")
    void deleteAllByUserId(UUID userId);
}
