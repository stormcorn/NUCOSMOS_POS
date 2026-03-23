package com.nucosmos.pos.backend.auth.repository;

import com.nucosmos.pos.backend.auth.persistence.UserEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface UserRepository extends JpaRepository<UserEntity, UUID> {

    List<UserEntity> findAllByOrderByEmployeeCodeAsc();

    boolean existsByEmployeeCode(String employeeCode);

    boolean existsByEmployeeCodeAndIdNot(String employeeCode, UUID id);

    Optional<UserEntity> findByEmployeeCode(String employeeCode);

    @Query("""
            select distinct u
            from UserEntity u
            join StoreStaffAssignmentEntity assignment on assignment.user = u
            join UserRoleEntity userRole on userRole.user = u
            join userRole.role role
            where assignment.store.code = :storeCode
              and assignment.active = true
              and u.status = 'ACTIVE'
              and role.code = :roleCode
              and role.active = true
            """)
    List<UserEntity> findCandidatesForPinLogin(String storeCode, String roleCode);
}
