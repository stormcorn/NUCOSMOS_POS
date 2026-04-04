package com.nucosmos.pos.backend.space.repository;

import com.nucosmos.pos.backend.space.persistence.SpaceResourceEntity;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface SpaceResourceRepository extends JpaRepository<SpaceResourceEntity, UUID> {

    @EntityGraph(attributePaths = {"store", "bookingPolicy"})
    List<SpaceResourceEntity> findAllByActiveTrueOrderByNameAsc();

    @EntityGraph(attributePaths = {"store", "bookingPolicy"})
    List<SpaceResourceEntity> findAllByStore_CodeAndActiveTrueOrderByNameAsc(String storeCode);

    @EntityGraph(attributePaths = {"store", "bookingPolicy"})
    Optional<SpaceResourceEntity> findBySlugAndActiveTrue(String slug);

    @EntityGraph(attributePaths = {"store", "bookingPolicy"})
    Optional<SpaceResourceEntity> findByIdAndStore_Code(UUID id, String storeCode);
}
