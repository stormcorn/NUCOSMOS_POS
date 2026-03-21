package com.nucosmos.pos.backend.product.repository;

import com.nucosmos.pos.backend.product.persistence.ProductCategoryEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ProductCategoryRepository extends JpaRepository<ProductCategoryEntity, UUID> {

    List<ProductCategoryEntity> findAllByOrderByDisplayOrderAscNameAsc();

    List<ProductCategoryEntity> findAllByActiveTrueOrderByDisplayOrderAscNameAsc();

    List<ProductCategoryEntity> findAllByActiveTrueAndUpdatedAtAfterOrderByDisplayOrderAscNameAsc(OffsetDateTime updatedAt);

    Optional<ProductCategoryEntity> findByIdAndActiveTrue(UUID id);

    boolean existsByCodeIgnoreCase(String code);

    boolean existsByCodeIgnoreCaseAndIdNot(String code, UUID id);
}
