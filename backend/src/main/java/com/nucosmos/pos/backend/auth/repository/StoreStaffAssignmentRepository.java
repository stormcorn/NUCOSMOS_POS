package com.nucosmos.pos.backend.auth.repository;

import com.nucosmos.pos.backend.auth.persistence.StoreStaffAssignmentEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.UUID;

public interface StoreStaffAssignmentRepository extends JpaRepository<StoreStaffAssignmentEntity, UUID> {

    @Query("""
            select assignment
            from StoreStaffAssignmentEntity assignment
            join fetch assignment.store store
            where assignment.user.id in :userIds
            order by store.code
            """)
    List<StoreStaffAssignmentEntity> findAllByUserIdIn(List<UUID> userIds);

    @Modifying
    @Query("delete from StoreStaffAssignmentEntity assignment where assignment.user.id = :userId")
    void deleteAllByUserId(UUID userId);
}
