package com.nucosmos.pos.backend.auth.repository;

import com.nucosmos.pos.backend.auth.persistence.RolePermissionEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.UUID;

public interface RolePermissionRepository extends JpaRepository<RolePermissionEntity, UUID> {

    @Query("""
            select permission
            from RolePermissionEntity permission
            join fetch permission.role role
            where role.id in :roleIds
            order by permission.permissionKey
            """)
    List<RolePermissionEntity> findAllByRoleIdIn(List<UUID> roleIds);

    @Modifying
    @Query("delete from RolePermissionEntity permission where permission.role.id = :roleId")
    void deleteAllByRoleId(UUID roleId);

    @Query("""
            select permission.permissionKey
            from RolePermissionEntity permission
            join permission.role role
            where role.code = :roleCode
            order by permission.permissionKey
            """)
    List<String> findPermissionKeysByRoleCode(String roleCode);
}
