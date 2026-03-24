package com.nucosmos.pos.backend.product.repository;

import com.nucosmos.pos.backend.product.persistence.ProductCustomizationGroupEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface ProductCustomizationGroupRepository extends JpaRepository<ProductCustomizationGroupEntity, UUID> {
    List<ProductCustomizationGroupEntity> findAllByProduct_IdOrderByDisplayOrderAscCreatedAtAsc(UUID productId);

    List<ProductCustomizationGroupEntity> findAllByProduct_IdAndActiveTrueOrderByDisplayOrderAscCreatedAtAsc(UUID productId);

    void deleteAllByProduct_Id(UUID productId);
}
